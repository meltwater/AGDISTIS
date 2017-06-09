package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.aksw.agdistis.AGDISTISConfigurationException;
import org.aksw.agdistis.Algorithm;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.PreprocessingNLP;
import org.aksw.agdistis.util.Stemming;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.aksw.agdistis.util.TripleIndexContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.search.spell.StringDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class CandidateUtil {

  private static Logger LOGGER = LoggerFactory.getLogger(CandidateUtil.class);
  private static final String THING_TYPE = "http://www.w3.org/2002/07/owl#Thing";
  private static final String DBPEDIA_TYPE_PREFIX = "http://dbpedia.org/ontology/";

  private final String nodeType;
  private final TripleIndex index;
  private TripleIndexContext index2;
  private final StringDistance metric;
  private final CorporationAffixCleaner corporationAffixCleaner;
  private final DomainWhiteLister preDisambiguationDomainWhiteLister;
  private final boolean popularity;
  private final Algorithm algorithm;
  private final boolean acronym;
  private final boolean commonEntities;
  private final Cache<String, Boolean> disambiguationCache = CacheBuilder.newBuilder().maximumSize(50000).build();
  private final Cache<String, List<Triple>> candidateCache = CacheBuilder.newBuilder().maximumSize(50000).build();
  private final static Stemming stemmer = new Stemming();

  public CandidateUtil() {
    try {
      nodeType = AGDISTISConfiguration.INSTANCE.getNodeType().toString();

      index = new TripleIndex();
      if (AGDISTISConfiguration.INSTANCE.getUseContext()) { // in case the index by context exist
        index2 = new TripleIndexContext();
      }
      metric = AGDISTISConfiguration.INSTANCE.getCandidatePruningMetric();
      corporationAffixCleaner = new CorporationAffixCleaner();
      preDisambiguationDomainWhiteLister = new DomainWhiteLister(index,
          AGDISTISConfiguration.INSTANCE.getPreDisambiguationWhiteListPath());
      popularity = AGDISTISConfiguration.INSTANCE.getUsePopularity();
      acronym = AGDISTISConfiguration.INSTANCE.getUseAcronym();
      commonEntities = AGDISTISConfiguration.INSTANCE.getUseCommonEntities();
      algorithm = AGDISTISConfiguration.INSTANCE.getAlgorithm();
    } catch (final IOException ioe) {
      throw new AGDISTISConfigurationException(
          "Unable to load configuration file. StackTrace: " + ExceptionUtils.getStackTrace(ioe));
    }
  }

  public void insertCandidatesIntoText(final DirectedSparseGraph<Node, String> graph, final Document document,
      final double threshholdTrigram, final Boolean heuristicExpansionOn, final Boolean useSurfaceForms)
      throws IOException {
    final NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
    final HashMap<String, Node> nodes = new HashMap<String, Node>();

    // used for heuristic label expansion start with longest Named Entities
    Collections.sort(namedEntities.getNamedEntities(), new NamedEntityLengthComparator());

    final StringBuilder sb = new StringBuilder();
    for (final NamedEntityInText namedEntity : namedEntities) {
      sb.append(namedEntity.getLabel());
      sb.append(" ");
    }
    final String entities = StringUtils.normalizeSpace(sb.toString());
    LOGGER.trace("entities" + entities);
    final HashSet<String> heuristicExpansion = new HashSet<String>();
    for (final NamedEntityInText entity : namedEntities) {
      LOGGER.debug("Disambiguating label: " + entity.getLabel());
      final long start = System.currentTimeMillis();
      // Heuristic expansion is a rough approximation of a coreference resolution.
      String expandedlabel = entity.getLabel();
      if (heuristicExpansionOn) {
        expandedlabel = heuristicExpansion(heuristicExpansion, entity.getLabel());
      }
      checkLabelCandidates(graph, threshholdTrigram, nodes, entity, expandedlabel, useSurfaceForms, entities);
      LOGGER.trace("Candidates for {} located in {} msecs.", entity.getLabel(), (System.currentTimeMillis() - start));
    }
  }

  private String heuristicExpansion(final HashSet<String> heuristicExpansion, String label) {
    String tmp = label;
    boolean expansion = false;
    for (final String key : heuristicExpansion) {
      if (key.contains(label)) {
        // take the shortest possible expansion
        if ((tmp.length() > key.length()) && (tmp != label)) {
          tmp = key;
          expansion = true;
          LOGGER.trace("Heuristic expansion: {} --> {}", label, key);
        }
        if ((tmp.length() < key.length()) && (tmp == label)) {
          tmp = key;
          expansion = true;
          LOGGER.trace("Heuristic expansion: {} --> {}", label, key);
        }
      }
    }
    label = tmp;
    if (!expansion) {
      heuristicExpansion.add(label);
    }
    return label;
  }

  public void addNodeToGraph(final DirectedSparseGraph<Node, String> graph, final HashMap<String, Node> nodes,
      final NamedEntityInText entity, final Triple c, final String candidateURL) throws IOException {
    final Node currentNode = new Node(candidateURL, 0, 0, algorithm);
    LOGGER.debug("CandidateURL: " + candidateURL);
    // candidates are connected to a specific label in the text via their
    // start position
    if (!graph.addVertex(currentNode)) {
      final int st = entity.getStartPos();
      if (nodes.get(candidateURL) != null) {
        nodes.get(candidateURL).addId(st);
      } else {
        LOGGER.error("This vertex couldn't be added because of an bug in Jung: " + candidateURL);
      }
    } else {
      currentNode.addId(entity.getStartPos());
      nodes.put(candidateURL, currentNode);
    }
  }

  private void checkLabelCandidates(final DirectedSparseGraph<Node, String> graph, final double threshholdTrigram,
      final HashMap<String, Node> nodes, final NamedEntityInText entity, final String expandedLabel,
      final boolean searchInSurfaceForms, final String entities) throws IOException {

    List<Triple> toBeAdded;
    String label = entity.getLabel();
    // Check the cache
    if (null == (toBeAdded = candidateCache.getIfPresent(label))) {
      List<Triple> candidates = new ArrayList<Triple>();
      List<Triple> acronymCandidatesTemp = new ArrayList<Triple>();
      List<Triple> acronymCandidatesTemp2 = new ArrayList<Triple>();
      List<Triple> candidatesContext = new ArrayList<Triple>();
      final List<Triple> candidatesContextbyLabel = new ArrayList<Triple>();
      final List<Triple> linkedsbyContext = new ArrayList<Triple>();
      int countFinalCandidates = 0;

      final PreprocessingNLP nlp = new PreprocessingNLP();
      // Label treatment
      label = corporationAffixCleaner.cleanLabelsfromCorporationIdentifier(label);
      LOGGER.debug("Clean label: {}", label);
      label = nlp.preprocess(label);
      // label treatment finished ->
      // searchByAcronym
      if (acronym == true) {
        if (label.equals(label.toUpperCase()) && (label.length() <= 4)) {
          acronymCandidatesTemp = searchbyAcronym(label, searchInSurfaceForms, entity.getType());
          for (final Triple triple : acronymCandidatesTemp) {
            acronymCandidatesTemp2 = searchAcronymByLabel(triple.getSubject(), searchInSurfaceForms, entity.getType());
            for (final Triple triple2 : acronymCandidatesTemp2) {
              if (metric.getDistance(triple.getSubject(), triple2.getObject()) > threshholdTrigram) {
                // iff it is a disambiguation resource, skip it
                if (isDisambiguationResource(triple2.getSubject())) {
                  continue;
                }
                // follow redirect
                triple2.setSubject(redirect(triple2.getSubject()));
                if (commonEntities == true) {
                  LOGGER.trace("Entity {} with url {} was added to the graph.", entity, triple2.getSubject());
                  addNodeToGraph(graph, nodes, entity, triple2, triple2.getSubject());
                  countFinalCandidates++;
                } else {
                  if (preDisambiguationDomainWhiteLister.fitsIntoDomain(triple2.getSubject())) {
                    LOGGER.trace("Entity {} with url {} was added to the graph.", entity, triple2.getSubject());
                    addNodeToGraph(graph, nodes, entity, triple2, triple2.getSubject());
                    countFinalCandidates++;
                  }
                }
              }
            }
            acronymCandidatesTemp2.clear();
          }
          LOGGER.debug("Found {} candidates for acronym {}. [surface form={}]", countFinalCandidates, label,
              searchInSurfaceForms);
        }
      }
      // searchByAcronymFinished

      if (countFinalCandidates == 0) {
        candidates = searchCandidatesByLabel(label, searchInSurfaceForms, "", popularity);
        if (searchInSurfaceForms) {
          LOGGER.debug("Found {} surface form candidates for label {}.", candidates.size(), label);
        } else {
          LOGGER.debug("Found {} candidates for label {}.", candidates.size(), label);
        }

        if (candidates.size() == 0) {
          if (label.endsWith("'s")) {
            // removing plural s
            label = label.substring(0, label.lastIndexOf("s"));
            candidates = searchCandidatesByLabel(label, searchInSurfaceForms, "", popularity);
            LOGGER.debug("No candidates founds for singularized label.");
          } else if (label.endsWith("s")) {
            // removing genitive s
            label = label.substring(0, label.lastIndexOf("'s"));
            candidates = searchCandidatesByLabel(label, searchInSurfaceForms, "", popularity);
            LOGGER.debug("No candidates founds after removing genitive.");
          }
        }

        // if the set of candidates is still empty, try chopping the label by camelcase.
        if (candidates.isEmpty() && (label.split(" ").length == 1)) {
          final String camelSplit = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(label), " ");
          label = camelSplit;
          candidates = searchCandidatesByLabel(camelSplit, searchInSurfaceForms, "", popularity);
          if (!candidates.isEmpty()) {

          }
          LOGGER.debug("Found {} candidates by splitting {} into {}.", candidates.size(), label, camelSplit);
        }

        if (candidates.isEmpty() && !label.equals(expandedLabel)) {
          candidates = searchCandidatesByLabel(expandedLabel, searchInSurfaceForms, "", popularity);
        }
        LOGGER.debug("Found {} candidates for expanded label  {} of label {}.", candidates.size(), expandedLabel,
            label);

        // If the set of candidates is still empty, here we apply stemming
        // technique
        if (candidates.isEmpty()) {

          final String temp = stemmer.stemming(label);
          if (StringUtils.isNotBlank(temp)) {
            candidates = searchCandidatesByLabel(temp, searchInSurfaceForms, "", popularity);
          }
          LOGGER.debug("Found {} candidates for stem  {} of label {}.", candidates.size(), temp, label);
        }

        // Prune candidates using string similarity.
        toBeAdded = Lists.newLinkedList();
        boolean added = false;
        for (final Triple c : candidates) {
          LOGGER.debug("Candidate triple to check: " + c);
          String candidateURL = c.getSubject();
          String surfaceForm = c.getObject();
          surfaceForm = nlp.preprocess(surfaceForm);
          // rule of thumb: no year numbers in candidates
          if (candidateURL.startsWith(nodeType)) {
            // trigram similarity
            if (c.getPredicate().equals("http://www.w3.org/2000/01/rdf-schema#label")) {

              if ((metric.getDistance(surfaceForm.toLowerCase(), label.toLowerCase()) < 1.0)
                  && !surfaceForm.equals(expandedLabel)) {
                // Here we set the similarity as maximum because rfds:label refers to the main reference of a given
                // resource
                continue;

              }
            } else if (!c.getPredicate().equals("http://www.w3.org/2000/01/rdf-schema#label")) {
              // Here the similarity is in accordance with the user's choice.
              if (metric.getDistance(surfaceForm, label) < threshholdTrigram) {
                continue;
              }
            } // if it is a disambiguation resource, skip it
            if (isDisambiguationResource(candidateURL)) {
              continue;
            }
            // follow redirect
            candidateURL = redirect(candidateURL);
            if (commonEntities == true) {
              // Domain white list does not apply. All entities are accepted.
              toBeAdded.add(c);
              added = true;
              LOGGER.trace("Entity {} with url {} was added to the graph.", entity, candidateURL);
              countFinalCandidates++;
            } else {
              if (preDisambiguationDomainWhiteLister.fitsIntoDomain(candidateURL)) {
                toBeAdded.add(c);
                added = true;
                LOGGER.trace("Entity {} with url {} was added to the graph.", entity, candidateURL);
                countFinalCandidates++;
              }
            }
          }
        }
        // Looking by context starts here.
        if (!added && !searchInSurfaceForms && AGDISTISConfiguration.INSTANCE.getUseContext()) {
          LOGGER.debug("searchByContext");
          candidatesContext = searchCandidatesByContext(entities, label); // looking
                                                                          // for
                                                                          // all
                                                                          // entities
                                                                          // together
          LOGGER.debug("\t\tnumber of candidates by context: " + candidatesContext.size());

          // taking all possibles SF for each resource found.
          if (candidatesContext != null) {
            for (final Triple triple : candidatesContext) {
              final String url = nodeType + triple.getPredicate();
              candidatesContextbyLabel.addAll(searchCandidatesByUrl(url, searchInSurfaceForms));
            }
          }

          // Here, we apply two filters for increasing the quality of
          // possible candidates
          for (final Triple c : candidatesContextbyLabel) {
            LOGGER.debug("Candidate triple to check: " + c);
            String candidateURL = c.getSubject();
            String cleanCandidateURL = candidateURL.replace(nodeType, "");
            cleanCandidateURL = nlp.preprocess(cleanCandidateURL);
            if (candidateURL.startsWith(nodeType)) {
              // trigram similarity over the URIS
              if (metric.getDistance(cleanCandidateURL, label) < 0.3) {
                continue;
              }
              // finding direct connections
              for (final Triple temp : candidatesContext) {
                final String candidateTemp = nodeType + temp.getPredicate();
                linkedsbyContext.addAll(searchbyConnections(candidateURL, candidateTemp));
              }
              // Only resources which have connections with others are
              // treated as possible candidates.
              if (linkedsbyContext.size() < 1) {
                continue;
              }
              // if it is a disambiguation resource, skip it
              if (isDisambiguationResource(candidateURL)) {
                continue;
              }
              // follow redirect
              candidateURL = redirect(candidateURL);
              // Enabling more types of entities as the previous step.
              if (commonEntities == true) {
                addNodeToGraph(graph, nodes, entity, c, candidateURL);
                added = true;
                countFinalCandidates++;
              } else {
                if (preDisambiguationDomainWhiteLister.fitsIntoDomain(candidateURL)) {
                  addNodeToGraph(graph, nodes, entity, c, candidateURL);
                  added = true;
                  countFinalCandidates++;
                }
              }
            }
            linkedsbyContext.clear();
          }
        }
        // Cache the results
        candidateCache.put(label, toBeAdded);

        // Looking for the given label among the set of surface forms.
        if (!added && !searchInSurfaceForms) {
          LOGGER.debug("Search using SF from disambiguation, redirects and from anchors web pages");
          checkLabelCandidates(graph, threshholdTrigram, nodes, entity, expandedLabel, true, entities);
        }

      }
    } else {
      LOGGER.trace("Candidate cache hit!");
    }
    // Add surviving candidates to the graph
    for (final Triple t : toBeAdded) {
      addNodeToGraph(graph, nodes, entity, t, t.getSubject());
    }
  }

  private List<Triple> searchCandidatesByLabel(final String label, final boolean searchInSurfaceFormsToo,
      final String type, final boolean popularity) {
    List<Triple> tmp = Lists.newLinkedList();
    final List<Triple> tmp2 = Lists.newLinkedList();
    final List<Triple> finalTmp = Lists.newLinkedList();
    ArrayList<Triple> candidatesScore = new ArrayList<Triple>();

    if (popularity) { // Frequency of entities.
      tmp.addAll(index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label, 500));
      if (searchInSurfaceFormsToo) {
        tmp.clear();
        tmp.addAll(index.search(null, "http://www.w3.org/2004/02/skos/core#altLabel", label, 500));
      }

      for (final Triple c : tmp) {
        tmp2.add(new Triple(c.getSubject(), c.getPredicate(), c.getObject()));
        final String uri = c.getSubject().replace(nodeType, "");
        candidatesScore = searchCandidatesByScore(uri);
        c.setPredicate(c.getObject());
        if (candidatesScore.isEmpty()) {
          c.setObject("1");
        } else {
          c.setObject(candidatesScore.get(0).getObject());
        }
      }

      Collections.sort(tmp);

      if (tmp.size() < 100) {
        for (final Triple triple : tmp.subList(0, tmp.size())) {
          for (final Triple triple2 : tmp2) {
            if (triple.getSubject().equals(triple2.getSubject()) && triple.getPredicate().equals(triple2.getObject())) {
              finalTmp.add(triple2);
              continue;
            }

          }
        }

      } else if (tmp.size() >= 100) {
        for (final Triple triple : tmp.subList(0, 100)) {
          for (final Triple triple2 : tmp2) {
            if (triple.getSubject().equals(triple2.getSubject()) && triple.getPredicate().equals(triple2.getObject())) {
              finalTmp.add(triple2);
              continue;
            }

          }
        }

      }
      return finalTmp;
    } else {
      tmp = index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label);
      if (searchInSurfaceFormsToo) {
        tmp.clear();
        tmp = index.search(null, "http://www.w3.org/2004/02/skos/core#altLabel", label);
      }
      return tmp;
    }
  }

  public List<Triple> searchbyAcronym(final String label, final boolean searchInSurfaceFormsToo, final String type) {
    return index.search(null, "http://dbpedia.org/property/acronym", label, 100);
  }

  public List<Triple> searchAcronymByLabel(final String label, final boolean searchInSurfaceFormsToo,
      final String type) {
    return index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label, 100);
  }

  ArrayList<Triple> searchCandidatesByContext(final String entities, final String label) {
    final ArrayList<Triple> tmp = new ArrayList<Triple>();
    tmp.addAll(index2.search(entities, label, null, 100));

    return tmp;
  }

  ArrayList<Triple> searchCandidatesByScore(final String label) {
    final ArrayList<Triple> tmp = new ArrayList<Triple>();
    tmp.addAll(index2.search(null, label, null));

    return tmp;
  }

  List<Triple> searchbyConnections(final String uri, final String uri2) {
    return index.search(uri, null, uri2);
  }

  List<Triple> searchCandidatesByUrl(final String url, final boolean searchInSurfaceFormsToo) {

    final List<Triple> tmp2 = Lists.newLinkedList();
    final ArrayList<Triple> finalTmp = new ArrayList<Triple>();
    ArrayList<Triple> candidatesScore = new ArrayList<Triple>();

    if (popularity) {
      final List<Triple> tmp = index.search(url, "http://www.w3.org/2000/01/rdf-schema#label", null, 500);

      for (final Triple c : tmp) {
        tmp2.add(new Triple(c.getSubject(), c.getPredicate(), c.getObject()));
        final String uri = c.getSubject().replace(nodeType, "");
        candidatesScore = searchCandidatesByScore(uri);
        c.setPredicate(c.getObject());
        if (candidatesScore.isEmpty()) {
          c.setObject("1");
        } else {
          c.setObject(candidatesScore.get(0).getObject());
        }
      }

      Collections.sort(tmp);

      if (tmp.size() < 100) {
        for (final Triple triple : tmp.subList(0, tmp.size())) {
          for (final Triple triple2 : tmp2) {
            if (triple.getSubject().equals(triple2.getSubject()) && triple.getPredicate().equals(triple2.getObject())) {
              finalTmp.add(triple2);
              continue;
            }

          }
        }

      } else if (tmp.size() >= 100) {
        for (final Triple triple : tmp.subList(0, 100)) {
          for (final Triple triple2 : tmp2) {
            if (triple.getSubject().equals(triple2.getSubject()) && triple.getPredicate().equals(triple2.getObject())) {
              finalTmp.add(triple2);
              continue;
            }

          }
        }

      }
      return finalTmp;
    } else {
      return index.search(url, "http://www.w3.org/2000/01/rdf-schema#label", null);
    }
  }

  public List<String> getDisambiguatedTypes(final String entityURI, final TripleIndex index) {
    final List<String> types = Lists.newLinkedList();

    // get the type from the redirection.
    final List<Triple> triples = index.search(redirect(entityURI), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
        null);

    for (final Triple triple : triples) {
      final String typeURI = triple.getObject();
      if (!typeURI.equals(THING_TYPE) && typeURI.startsWith(DBPEDIA_TYPE_PREFIX)) {
        types.add(typeURI);
      }
    }
    return types;
  }

  private boolean isDisambiguationResource(final String candidateURL) {

    final Boolean in = disambiguationCache.getIfPresent(candidateURL);
    if (in != null) {
      return in;
    }

    final List<Triple> tmp = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageDisambiguates", null);
    if (tmp.isEmpty()) {
      disambiguationCache.put(candidateURL, false);
      return false;
    } else {
      disambiguationCache.put(candidateURL, true);
      return true;
    }
  }

  private String redirect(final String candidateURL) {
    if (candidateURL == null) {
      return candidateURL;
    }
    final List<Triple> redirect = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageRedirects", null);
    if (redirect.size() == 1) {
      return redirect.get(0).getObject();
    } else if (redirect.size() > 1) {
      LOGGER.warn("Several redirects detected for :" + candidateURL);
      return candidateURL;
    } else {
      return candidateURL;
    }
  }

  public void close() throws IOException {
    index.close();
  }

  public TripleIndex getIndex() {
    return index;
  }

}
