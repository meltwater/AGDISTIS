package org.aksw.agdistis.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class AGDISTIS {

  private final Logger log = LoggerFactory.getLogger(AGDISTIS.class);
  private final String edgeType;
  private final String nodeType;
  // private CandidateBKP cu;
  private final CandidateUtil cu;
  private final TripleIndex index;
  // needed for the experiment about which properties increase accuracy
  private final double threshholdTrigram;
  private final int maxDepth;
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
  }

  public void run(final Document document, final Map<NamedEntityInText, List<CandidatesScore>> candidatesPerNE) {
    try {
      final NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
      final DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

      // 0) insert candidates into Text
      log.debug("\tinsert candidates");
      cu.insertCandidatesIntoText(graph, document, threshholdTrigram, heuristicExpansionOn, useSurfaceForms);

      // 1) let spread activation/ breadth first search run
      log.info("\tGraph size before BFS: " + graph.getVertexCount());
      final BreadthFirstSearch bfs = new BreadthFirstSearch(index, algorithm);
      bfs.run(maxDepth, graph, edgeType, nodeType);
      log.info("\tGraph size after BFS: " + graph.getVertexCount());

      if (algorithm == Algorithm.HITS) {
        // 2.1) let HITS run
        log.info("\trun HITS");
        final HITS h = new HITS();
        h.runHits(graph, 20);
      } else if (algorithm == Algorithm.PAGERANK) {
        // 2.2) let Pagerank run
        log.info("\trun PageRank");
        final PageRank pr = new PageRank();
        pr.runPr(graph, 50, 0.1);
      }

      // 3) store the candidate with the highest hub, highest authority
      // ratio
      // manipulate which value to use directly in node.compareTo
      log.debug("\torder results");
      final ArrayList<Node> orderedList = new ArrayList<Node>();
      orderedList.addAll(graph.getVertices());
      Collections.sort(orderedList);
      for (final NamedEntityInText entity : namedEntities) {
        for (int i = 0; i < orderedList.size(); i++) {
          final Node m = orderedList.get(i);
          // there can be one node (candidate) for two labels
          if (m.containsId(entity.getStartPos())) {
            entity.setNamedEntity(m.getCandidateURI());
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

    } catch (final Exception e) {
      log.error("AGDISTIS cannot be run on this document.", e);
    }
  }

}
