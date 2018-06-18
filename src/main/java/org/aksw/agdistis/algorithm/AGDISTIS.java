package org.aksw.agdistis.algorithm;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import net.logstash.logback.marker.Markers;
import org.aksw.agdistis.AGDISTISConfiguration;
import org.aksw.agdistis.Algorithm;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.graph.BreadthFirstSearch;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.graph.PageRank;
import org.aksw.agdistis.model.CandidatesScore;
import org.aksw.agdistis.util.TripleIndex;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AGDISTIS {

    private final Logger LOGGER = LoggerFactory.getLogger(AGDISTIS.class);
    private final String edgeType;
    private final String nodeType;
    private final CandidateUtil cu;
    private final TripleIndex index;
    private final double threshholdTrigram;
    private final int maxDepth;
    private final DomainWhiteLister postDisambiguationDomainWhiteLister;
    private final boolean heuristicExpansionOn;
    private final boolean useSurfaceForms;
    private final Algorithm algorithm;

    public AGDISTIS() {
        heuristicExpansionOn = AGDISTISConfiguration.INSTANCE.getHeuristicExpansion();
        algorithm = AGDISTISConfiguration.INSTANCE.getAlgorithm();

        nodeType = AGDISTISConfiguration.INSTANCE.getNodeType().toString();
        edgeType = AGDISTISConfiguration.INSTANCE.getEdgeType().toString();
        threshholdTrigram = AGDISTISConfiguration.INSTANCE.getCandidatePruningThreshold();
        maxDepth = AGDISTISConfiguration.INSTANCE.getSemanticDepth();
        useSurfaceForms = AGDISTISConfiguration.INSTANCE.getUseSurfaceForms();
        cu = new CandidateUtil();
        index = cu.getIndex();

        postDisambiguationDomainWhiteLister = new DomainWhiteLister(index, AGDISTISConfiguration.INSTANCE
                .getPostDisambiguationWhiteListPath());
    }

    public void run(final Document document, final Map<NamedEntityInText, List<CandidatesScore>> candidatesPerNE) {
        try {

            final NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
            final DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

            LOGGER.debug("Selecting candidates.");
            long start = System.currentTimeMillis();
            cu.insertCandidatesIntoText(graph, document, threshholdTrigram, heuristicExpansionOn, useSurfaceForms);
            final long candidateSelectionTime = System.currentTimeMillis() - start;

            LOGGER.debug("Performing graph-based disambiguation.");
            start = System.currentTimeMillis();
            // 1) let spread activation/ breadth first search run
            LOGGER.trace("Graph size before BFS: " + graph.getVertexCount());
            final BreadthFirstSearch bfs = new BreadthFirstSearch(index, algorithm);
            bfs.run(maxDepth, graph, edgeType, nodeType);
            LOGGER.trace("Graph size after BFS: " + graph.getVertexCount());

            if (algorithm == Algorithm.HITS) {
                // 2.1) let HITS run
                LOGGER.debug("Run HITS");
                final HITS h = new HITS();
                h.runHits(graph, 20);
            }
            else if (algorithm == Algorithm.PAGERANK) {
                // 2.2) let Pagerank run
                LOGGER.debug("Run PAGERANK");
                final PageRank pr = new PageRank();
                pr.runPr(graph, 50, 0.1);
            }

            // 3) store the candidate with the highest hub, highest authority
            // ratio
            // manipulate which value to use directly in node.compareTo
            LOGGER.trace("Sort results");
            final ArrayList<Node> orderedList = new ArrayList<Node>();
            orderedList.addAll(graph.getVertices());
            Collections.sort(orderedList);
            for (final NamedEntityInText entity : namedEntities) {
                for (final Node m : orderedList) {
                    // there can be one node (candidate) for two labels
                    if (m.containsId(entity.getStartPos()) && postDisambiguationDomainWhiteLister.fitsIntoDomain(m
                            .getCandidateURI(), Optional.of(entity.getType()))) {
                        final String candidateURI = m.getCandidateURI();
                        final String canonicalName = m.getLabelString();
                        entity.setCanonicalName(canonicalName);
                        entity.setNamedEntity(candidateURI);
                        entity.setDisambiguatedTypes(cu.getDisambiguatedTypes(m.getCandidateURI(), index));
                        if (algorithm == Algorithm.HITS) {
                            entity.setAuthorityWeight(m.getUnnormalizedAuthorityWeight());
                            entity.setHubWeight(m.getUnnormalizedHubWeight());
                        }
                        else if (algorithm == Algorithm.PAGERANK) {
                            entity.setAuthorityWeight(m.getPageRank());
                        }
                        break;
                    }
                }
            }
            // To get all candidates along with their scores
            if (candidatesPerNE != null) {
                final List<CandidatesScore> listCandidates = new ArrayList<>();
                for (final NamedEntityInText entity : namedEntities) {
                    for (int i = 0; i < orderedList.size(); i++) {
                        final Node m = orderedList.get(i);

                        // there can be one node (candidate) for two labels
                        if (m.containsId(entity.getStartPos())) {

                            final CandidatesScore candidates = new CandidatesScore();
                            candidates.setStart(entity.getStartPos());
                            candidates.setUri(m.getCandidateURI());
                            candidates.setScore(m.getAuthorityWeight());
                            listCandidates.add(candidates);
                        }

                    }
                    candidatesPerNE.put(entity, listCandidates);
                }
            }
            final long disambiguationTime = System.currentTimeMillis() - start;
            document.setDisambiguationTime(disambiguationTime);
            document.setCandidateSelectionTime(candidateSelectionTime);
            document.setAGDISTISVersion(AGDISTISConfiguration.INSTANCE.getAGDISTISVersion());

            LOGGER.debug("Candidates computed in {} msecs", candidateSelectionTime);
            LOGGER.debug("Disambiguation completed in {} msecs.", disambiguationTime);

        } catch (final Exception e) {
            LOGGER.error(Markers.append("docId", document.getDocumentId()), "AGDISTIS cannot be run on this document"
                    + ".", ExceptionUtils.getStackTrace(e));
        }
    }

    public TripleIndex getIndex() {
        return cu.getIndex();
    }
}
