package org.aksw.agdistis.graph;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.aksw.agdistis.Algorithm;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.apache.commons.lang3.StringUtils;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class BreadthFirstSearch {
  private static HashMap<String, Node> findNode = new HashMap<String, Node>();
  private final TripleIndex index;
  private final Algorithm algorithm;
  private final int _MAX_OUTGOING_NODES = 100;

  public BreadthFirstSearch(final TripleIndex index, final Algorithm algorithm) {
    this.index = index;
    this.algorithm = algorithm;
  }

  public void run(final int maxDepth, final DirectedSparseGraph<Node, String> graph, final String edgeType,
      final String nodeType) throws UnsupportedEncodingException, IOException {
    final Queue<Node> q = new LinkedList<Node>();
    for (final Node node : graph.getVertices()) {
      findNode.put(node.getCandidateURI(), node);
      q.add(node);
    }
    while (!q.isEmpty()) {
      final Node currentNode = q.poll();
      final int level = currentNode.getLevel();
      if (level < maxDepth) {
        List<Triple> outgoingNodes = null;
        outgoingNodes = index.search(currentNode.getCandidateURI(), null, null, _MAX_OUTGOING_NODES);
        if (outgoingNodes == null) {
          continue;
        }
        for (final Triple targetNode : outgoingNodes) {
          if ((targetNode.getPredicate() == null) && (targetNode.getObject() == null)) {
            continue;
          }
          if (targetNode.getPredicate().startsWith(edgeType) && targetNode.getObject().startsWith(nodeType)) {
            final int levelNow = level + 1;
            Node node = null;
            if (findNode.containsKey(targetNode.getObject())) {
              node = findNode.get(targetNode.getObject());
            } else {
              node = new Node(targetNode.getObject(), 0, levelNow, algorithm);
              findNode.put(targetNode.getObject(), node);
              q.add(node);
            }
            graph.addEdge(StringUtils.join(graph.getEdgeCount(), ";", targetNode.getPredicate()), currentNode, node);
          }
        }
      }
    }
  }
}
