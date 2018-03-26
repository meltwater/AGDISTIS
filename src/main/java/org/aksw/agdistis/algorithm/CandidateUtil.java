package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
  private final Cache<String, Boolean> disambiguationPageCache = CacheBuilder.newBuilder()
      .maximumSize(AGDISTISConfiguration.INSTANCE.getDisambiguationPageCacheSize())
      .expireAfterWrite(30, TimeUnit.MINUTES).build();

  private final Cache<String, List<Triple>> candidateCache = CacheBuilder.newBuilder()
      .maximumSize(AGDISTISConfiguration.INSTANCE.getCandidateCacheSize()).expireAfterWrite(30, TimeUnit.MINUTES)
      .build();
  private final static Stemming stemmer = new Stemming();

  private final static int _MAX_CANDIDATE_LOOKUPS = AGDISTISConfiguration.INSTANCE.getMaxCandidateLookups();
  private final static int _MAX_RETRIEVED_ACRONYMS = AGDISTISConfiguration.INSTANCE.getMaxAcronymLookups();
  private final static int _MAX_RETRIEVED_CONNECTIONS = AGDISTISConfiguration.INSTANCE.getMaxConnectionLookups();;

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
    LOGGER.trace("Entities: {}", entities);
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
      final HashMap<String, Node> nodes, final NamedEntityInText entity, final String expandedSurfaceForm,
      final boolean alternativeLabels, final String entities) throws IOException {

    List<Triple> toBeAdded;
    String surfaceForm = entity.getSurfaceForm();
    // Check the cache
    if (null == (toBeAdded = candidateCache.getIfPresent(surfaceForm))) {
      List<Triple> candidates = new ArrayList<Triple>();
      List<Triple> acronymCandidatesTemp = new ArrayList<Triple>();
      List<Triple> acronymCandidatesTemp2 = new ArrayList<Triple>();
      List<Triple> candidatesContext = new ArrayList<Triple>();
      final List<Triple> candidatesContextbyLabel = new ArrayList<Triple>();
      final List<Triple> linkedsbyContext = new ArrayList<Triple>();
      int countFinalCandidates = 0;

      // Surface form cleaning.
      final PreprocessingNLP nlp = new PreprocessingNLP();

      surfaceForm = corporationAffixCleaner.cleanLabelsfromCorporationIdentifier(surfaceForm);
      LOGGER.debug("Clean label: {}", surfaceForm);
      surfaceForm = nlp.preprocess(surfaceForm);
      // label treatment finished ->
      // searchByAcronym
      if (acronym == true) {
        if (surfaceForm.equals(surfaceForm.toUpperCase()) && (surfaceForm.length() <= 4)) {
          acronymCandidatesTemp = searchbyAcronym(surfaceForm, alternativeLabels, entity.getType());
          for (final Triple triple : acronymCandidatesTemp) {
            acronymCandidatesTemp2 = searchAcronymByLabel(triple.getSubject(), alternativeLabels, entity.getType());
            for (final Triple triple2 : acronymCandidatesTemp2) {
              if (metric.getDistance(triple.getSubject(), triple2.getObject()) > threshholdTrigram) {
                // iff it is a disambiguation resource, skip it
                if (isDisambiguationResource(triple2.getSubject())) {
                  continue;
                }
                // follow redirect
                triple2.setSubject(redirect(triple2.getSubject()));
                if (commonEntities == true) {
                  addNodeToGraph(graph, nodes, entity, triple2, triple2.getSubject());
                  LOGGER.trace("Entity {} with url {} was added to the graph.", entity, triple2.getSubject());
                  countFinalCandidates++;
                } else {
                  if (preDisambiguationDomainWhiteLister.fitsIntoDomain(triple2.getSubject(),
                      Optional.of(entity.getType()))) {
                    addNodeToGraph(graph, nodes, entity, triple2, triple2.getSubject());
                    LOGGER.trace("Entity {} with url {} was added to the graph.", entity, triple2.getSubject());
                    countFinalCandidates++;
                  }
                }
              }
            }
            acronymCandidatesTemp2.clear();
          }
          LOGGER.debug("Found {} candidates for acronym {}. [surface form={}]", countFinalCandidates, surfaceForm,
              alternativeLabels);
        }
      }

      // Search by standard label
      if (countFinalCandidates == 0) {
        candidates = searchCandidatesByLabel(surfaceForm, alternativeLabels, entity.getType(), popularity);
        if (alternativeLabels) {
          LOGGER.debug("Found {} surface form candidates for label '{}'.", candidates.size(), surfaceForm);
        } else {
          LOGGER.debug("Found {} candidates for label '{}'.", candidates.size(), surfaceForm);
        }

        if (candidates.size() == 0) {
          if (surfaceForm.endsWith("'s")) {
            // removing genitive s
            surfaceForm = surfaceForm.substring(0, surfaceForm.lastIndexOf("'s"));
            candidates = searchCandidatesByLabel(surfaceForm, alternativeLabels, entity.getType(), popularity);
            LOGGER.debug("No candidates founds after removing genitive.");
          } else if (surfaceForm.endsWith("s")) {
            // removing plural s
            surfaceForm = surfaceForm.substring(0, surfaceForm.lastIndexOf("s"));
            candidates = searchCandidatesByLabel(surfaceForm, alternativeLabels, entity.getType(), popularity);
            LOGGER.debug("No candidates founds for singularized label.");
          }
        }

        // If the set of candidates is still empty, try chopping the label by camelcase.
        if (candidates.isEmpty() && (surfaceForm.split(" ").length == 1)) {
          final String camelSplit = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(surfaceForm), " ");
          surfaceForm = camelSplit;
          candidates = searchCandidatesByLabel(camelSplit, alternativeLabels, entity.getType(), popularity);
          if (!candidates.isEmpty()) {
            LOGGER.debug("Found '{}' candidates by splitting '{}' into '{}'.", candidates.size(), surfaceForm,
                camelSplit);
          }
        }

        if (candidates.isEmpty() && !surfaceForm.equals(expandedSurfaceForm)) {
          candidates = searchCandidatesByLabel(expandedSurfaceForm, alternativeLabels, entity.getType(), popularity);
          LOGGER.debug("Found {} candidates for expanded label  '{}' of label '{}'.", candidates.size(), expandedSurfaceForm,
              surfaceForm);
        }

        // If the set of candidates is still empty, here we apply stemming
        // technique
        if (candidates.isEmpty()) {
          final String temp = stemmer.stemming(surfaceForm);
          if (StringUtils.isNotBlank(temp)) {
            candidates = searchCandidatesByLabel(temp, alternativeLabels, entity.getType(), popularity);
          }
          LOGGER.debug("Found {} candidates for stem  {} of label {}.", candidates.size(), temp, surfaceForm);
        }

        // Prune candidates using string similarity.
        toBeAdded = Lists.newLinkedList();
        boolean added = false;
        for (Triple c : candidates) {
          LOGGER.debug("Candidate triple to check: " + c);
          String candidateURL = c.getSubject();
          String cleanLabel = c.getObject();

          // FIXME: This is a workaround for the aggressive cleaning of the labels in the index.
          if (!c.getSubject().contains("/fhai/")) {
            cleanLabel = StringUtils.normalizeSpace(
                StringUtils.replaceChars(StringUtils.substringAfter(c.getSubject(), "resource/"), "_()", "   "));
          }
          // End of the fix.

          cleanLabel = corporationAffixCleaner.cleanLabelsfromCorporationIdentifier(cleanLabel);
          cleanLabel = nlp.preprocess(cleanLabel);

          // rule of thumb: no year numbers in candidates
          if (candidateURL.startsWith(nodeType)) {
            // Trigram similarity.
            if ((metric.getDistance(cleanLabel.toLowerCase(), surfaceForm.toLowerCase()) < threshholdTrigram)) {
              if (expandedSurfaceForm.equals(cleanLabel)
                  || (metric.getDistance(cleanLabel.toLowerCase(), expandedSurfaceForm.toLowerCase()) < threshholdTrigram)) {
                continue;
              }

            }
            // If this is a disambiguation resource, skip it.
            if (isDisambiguationResource(candidateURL)) {
              continue;
            }
            // Check if this candidateURL redirects to something else, accept only if search among alternative labels is
            // enabled.
            final String redirectedURL = redirect(candidateURL);
            if (!candidateURL.equals(redirectedURL)) {
              // Replace with the redirected triple.
              final List<Triple> redirected = index.search(redirectedURL, c.getPredicate(), null);
              if (!redirected.isEmpty()) {
                c = redirected.get(0);
                candidateURL = c.getSubject();
              }
            }
            if (commonEntities == true) {
              // Domain white list does not apply. All entities are accepted.
              if (!toBeAdded.contains(c)) {
                toBeAdded.add(c);
              }
              added = true;
              LOGGER.trace("Entity {} with url {} was added to the graph.", entity, candidateURL);
              countFinalCandidates++;
            } else {
              if (preDisambiguationDomainWhiteLister.fitsIntoDomain(candidateURL, Optional.of(entity.getType()))) {
                if (!toBeAdded.contains(c)) {
                  toBeAdded.add(c);
                }
                added = true;
                LOGGER.trace("Entity {} with url {} was added to the graph.", entity, candidateURL);
                countFinalCandidates++;
              }
            }
          }
        }
        // Looking by context starts here.
        if (!added && !alternativeLabels && AGDISTISConfiguration.INSTANCE.getUseContext()) {
          LOGGER.debug("searchByContext");
          candidatesContext = searchCandidatesByContext(entities, surfaceForm); // looking
          // for
          // all
          // entities
          // together
          LOGGER.debug("\t\tnumber of candidates by context: " + candidatesContext.size());

          // taking all possibles SF for each resource found.
          if (candidatesContext != null) {
            for (final Triple triple : candidatesContext) {
              final String url = nodeType + triple.getPredicate();
              candidatesContextbyLabel.addAll(searchCandidatesByUrl(url, alternativeLabels));
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
              if (metric.getDistance(cleanCandidateURL, surfaceForm) >= 0.3) {
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
                if (preDisambiguationDomainWhiteLister.fitsIntoDomain(candidateURL, Optional.of(entity.getType()))) {
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
        candidateCache.put(surfaceForm, toBeAdded);

        // Looking for the given label among the set of surface forms.
        if (!added && alternativeLabels) {
          LOGGER.debug("Search using SF from disambiguation, redirects and from anchors web pages");
          checkLabelCandidates(graph, threshholdTrigram, nodes, entity, expandedSurfaceForm, true, entities);
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

  private List<Triple> searchCandidatesByLabel(final String label, final boolean searchAlternativeLabels,
      final String type, final boolean popularity) {

    if (popularity) {
      // Frequency of entities.
      final List<Triple> tmp = Lists.newLinkedList();
      final List<Triple> tmp2 = Lists.newLinkedList();
      final List<Triple> finalTmp = Lists.newLinkedList();
      ArrayList<Triple> candidatesScore = new ArrayList<Triple>();
      tmp.addAll(index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label, _MAX_CANDIDATE_LOOKUPS));
      if (searchAlternativeLabels) {
        for (final Triple t : index.search(null, "http://www.w3.org/2004/02/skos/core#altLabel", label,
            _MAX_CANDIDATE_LOOKUPS)) {
          tmp.add(t);
        }
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
      return Lists.newLinkedList(finalTmp);
    } else {
      final Set<Triple> tmp = new LinkedHashSet<Triple>();
      tmp.addAll(index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label, _MAX_CANDIDATE_LOOKUPS));
      if (searchAlternativeLabels) {
        for (final Triple t : index.search(null, "http://www.w3.org/2004/02/skos/core#altLabel", label,
            _MAX_CANDIDATE_LOOKUPS)) {
          tmp.add(t);
        }
      }
      return Lists.newLinkedList(tmp);
    }
  }

  public List<Triple> searchbyAcronym(final String label, final boolean searchInSurfaceFormsToo, final String type) {
    return index.search(null, "http://dbpedia.org/property/acronym", label, _MAX_RETRIEVED_ACRONYMS);
  }

  public List<Triple> searchAcronymByLabel(final String acronym, final boolean searchInSurfaceFormsToo,
      final String type) {
    return index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", acronym, _MAX_RETRIEVED_ACRONYMS);
  }

  ArrayList<Triple> searchCandidatesByContext(final String entities, final String label) {
    final ArrayList<Triple> tmp = new ArrayList<Triple>();
    tmp.addAll(index2.search(entities, label, null, _MAX_CANDIDATE_LOOKUPS));

    return tmp;
  }

  ArrayList<Triple> searchCandidatesByScore(final String label) {
    final ArrayList<Triple> tmp = new ArrayList<Triple>();
    tmp.addAll(index2.search(null, label, null));

    return tmp;
  }

  List<Triple> searchbyConnections(final String uri, final String uri2) {
    return index.search(uri, null, uri2, _MAX_RETRIEVED_CONNECTIONS);
  }

  List<Triple> searchCandidatesByUrl(final String url, final boolean searchInSurfaceFormsToo) {

    final List<Triple> tmp2 = Lists.newLinkedList();
    final ArrayList<Triple> finalTmp = new ArrayList<Triple>();
    ArrayList<Triple> candidatesScore = new ArrayList<Triple>();

    if (popularity) {
      final List<Triple> tmp = index.search(url, "http://www.w3.org/2000/01/rdf-schema#label", null, 10);

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
      return index.search(url, "http://www.w3.org/2000/01/rdf-schema#label", null, 10);
    }
  }

  public List<String> getDisambiguatedTypes(final String entityURI, final TripleIndex index) {
    final List<String> types = Lists.newLinkedList();

    // get the type from the redirection.
    final List<Triple> triples = index.search(redirect(entityURI), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
        null, 20);

    for (final Triple triple : triples) {
      final String typeURI = triple.getObject();
      if (!typeURI.equals(THING_TYPE) && typeURI.startsWith(DBPEDIA_TYPE_PREFIX)) {
        types.add(typeURI);
      }
    }
    return types;
  }

  private boolean isDisambiguationResource(final String candidateURL) {

    final Boolean in = disambiguationPageCache.getIfPresent(candidateURL);
    if (in != null) {
      return in;
    }

    final List<Triple> tmp = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageDisambiguates", null, 1);
    if (tmp.isEmpty()) {
      disambiguationPageCache.put(candidateURL, false);
      return false;
    } else {
      disambiguationPageCache.put(candidateURL, true);
      return true;
    }
  }

  private String redirect(final String candidateURL) {
    if (candidateURL == null) {
      return candidateURL;
    }
    final List<Triple> redirect = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageRedirects", null, 1);
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
