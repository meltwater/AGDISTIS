package org.aksw.agdistis.graph;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;

import org.aksw.agdistis.Algorithm;
import org.apache.commons.lang3.StringUtils;

public class Node implements Comparable<Node> {

  private final HashSet<Integer> ids;
  private double activation;
  private String candidateURI;
  private String candidateType;
  private String labelString;
  private int level;
  private double hubWeightForCalculation = 1;
  private double authorityWeightForCalculation = 1;
  private double unnormalizedHubWeight;
  private double unnormalizedAuthorityWeight;
  private double hubWeight;
  private double authorityWeight;
  private double pageRank;
  private double pageRankNew;
  private final Algorithm algorithm;

  private final HashSet<Node> predecessors;
  private final HashSet<Node> successors;

  public Node(final String uri,final String candidateType, final String labelString, final double activation, final int level, final Algorithm algorithm)
      throws IOException {
    candidateURI = uri;
    this.candidateType = candidateType;
    this.labelString = labelString;
    this.activation = activation;
    this.level = level;
    hubWeight = 1;
    authorityWeight = 1;
    ids = new HashSet<Integer>();
    successors = new HashSet<Node>();
    predecessors = new HashSet<Node>();
    pageRank = 0;
    this.algorithm = algorithm;
  }
  
  public Node(final String uri, final double activation, final int level, final Algorithm algorithm)
          throws IOException {
        candidateURI = uri;
        this.activation = activation;
        this.level = level;
        hubWeight = 1;
        authorityWeight = 1;
        ids = new HashSet<Integer>();
        successors = new HashSet<Node>();
        predecessors = new HashSet<Node>();
        pageRank = 0;
        this.algorithm = algorithm;
      }

  @Override
  public String toString() {
    final DecimalFormat df = new DecimalFormat("#.####");
    return StringUtils.join(candidateURI, ":", String.valueOf(df.format(activation)), " H: ",
        String.valueOf(df.format(hubWeight)), " A: ", String.valueOf(df.format(authorityWeight)), " PR: ",
        String.valueOf(df.format(pageRank)));
  }

  @Override
  public int hashCode() {
    return candidateURI.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (hashCode() == o.hashCode()) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  // this determines the ordering of candidates and thus the result of the
  // algorithm
  // change to hub score
  public int compareTo(final Node m) {

    if (m.algorithm == Algorithm.HITS) {
      // System.out.println("AuthorityWeight");
      if ((m.getAuthorityWeight()) == (getAuthorityWeight())) {
        return 0;
      } else if ((m.getAuthorityWeight()) > (getAuthorityWeight())) {
        return 1;
      } else {
        return -1;
      }
    } else if (m.algorithm == Algorithm.PAGERANK) {
      // System.out.println("PageRank compareTo");
      if (m.getPageRank() == getPageRank()) {
        return 0;
      } else if (m.getPageRank() > getPageRank()) {
        return 1;
      } else {
        return -1;
      }
    } else {
      return -1;
    }
  }

  public boolean containsId(final int id) {
    return ids.contains(id);
  }

  public void addId(final int id) {
    ids.add(id);
  }

  public HashSet<Integer> getId() {
    return ids;
  }

  public String getCandidateURI() {
    return candidateURI;
  }

  public void setCandidateURI(final String uri) {
    candidateURI = uri;
  }
  
  public String getLabelString() {
      return labelString;
  }
  
  public String getCandidateType() {
      return candidateType;
  }

  public void setActivation(final double activation) {
    this.activation = activation;

  }

  public double getActivation() {
    return activation;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(final int i) {
    level = i;

  }

  public double getHubWeight() {
    return hubWeight;
  }

  public void setAuthorityWeight(final double x) {
    authorityWeight = x;

  }

  public double getAuthorityWeight() {
    return authorityWeight;
  }

  public void setHubWeight(final double y) {
    hubWeight = y;

  }

  public Integer[] getLabels() {
    return ids.toArray(new Integer[ids.size()]);
  }

  public HashSet<Node> getSuccessors() {
    return successors;
  }

  public HashSet<Node> getPredecessors() {
    return predecessors;
  }

  public void addPredecessor(final Node first) {
    predecessors.add(first);
  }

  public void addSuccesor(final Node second) {
    successors.add(second);
  }

  public void removeSuccesor(final Node right) {
    successors.remove(right);
  }

  public void removePredecessor(final Node left) {
    predecessors.remove(left);
  }

  public double getHubWeightForCalculation() {
    return hubWeightForCalculation;
  }

  public void setHubWeightForCalculation(final double hubWeightForCalculation) {
    this.hubWeightForCalculation = hubWeightForCalculation;
  }

  public double getAuthorityWeightForCalculation() {
    return authorityWeightForCalculation;
  }

  public void setAuthorityWeightForCalculation(final double authorityWeightForCalculation) {
    this.authorityWeightForCalculation = authorityWeightForCalculation;
  }

  public double getUnnormalizedHubWeight() {
    return unnormalizedHubWeight;
  }

  public void setUnnormalizedHubWeight(final double unnormalizedHubWeight) {
    this.unnormalizedHubWeight = unnormalizedHubWeight;
  }

  public double getUnnormalizedAuthorityWeight() {
    return unnormalizedAuthorityWeight;
  }

  public void setUnnormalizedAuthorityWeight(final double unnormalizedAuthorityWeight) {
    this.unnormalizedAuthorityWeight = unnormalizedAuthorityWeight;
  }

  public double getPageRank() {
    return pageRank;
  }

  public void setPageRank(final double pageRank) {
    this.pageRank = pageRank;
  }

  public double getPageRankNew() {
    return pageRankNew;
  }

  public void setPageRankNew(final double pageRankNew) {
    this.pageRankNew = pageRankNew;
  }

}
