package org.aksw.agdistis.graph;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Properties;

public class Node implements Comparable<Node> {

  private final HashSet<Integer> ids;
  private double activation;
  private String candidateURI;
  private int level;
  private double hubWeightForCalculation = 1;
  private double authorityWeightForCalculation = 1;
  private double unnormalizedHubWeight;
  private double unnormalizedAuthorityWeight;
  private double hubWeight;
  private double authorityWeight;
  private double pageRank;
  private double pageRankNew;
  private final static Properties prop = new Properties();
  private static String algorithm;
  private final static String _DEFAULT_ALGORITHM = "hits";

  static {
    try {
      prop.load(Node.class.getResourceAsStream("/config/agdistis.properties"));
      algorithm = prop.getProperty("algorithm");
    } catch (final IOException ioe) {
      algorithm = _DEFAULT_ALGORITHM;
    }
  }

  private final HashSet<Node> predecessors;
  private final HashSet<Node> successors;

  public Node(final String uri, final double activation, final int level) throws IOException {
    candidateURI = uri;
    this.activation = activation;
    this.level = level;
    hubWeight = 1;
    authorityWeight = 1;
    ids = new HashSet<Integer>();
    successors = new HashSet<Node>();
    predecessors = new HashSet<Node>();
    pageRank = 0;
  }

  @Override
  public String toString() {
    final DecimalFormat df = new DecimalFormat("#.####");
    return candidateURI + ":" + String.valueOf(df.format(activation)) + " H: " + String.valueOf(df.format(hubWeight)
        + " A: " + String.valueOf(df.format(authorityWeight) + " PR: " + String.valueOf(df.format(pageRank))));
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

    if (algorithm.equals("hits")) {
      // System.out.println("AuthorityWeight");
      if (m.getAuthorityWeight() == getAuthorityWeight()) {
        return 0;
      } else if (m.getAuthorityWeight() > getAuthorityWeight()) {
        return 1;
      } else {
        return -1;
      }
    } else if (algorithm.equals("pagerank")) {
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
    // HubWeight
    // if (m.getHubWeight() == this.getHubWeight()) {
    // return 0;
    // } else if (m.getHubWeight() > this.getHubWeight()) {
    // return 1;
    // } else {
    // return -1;
    // }

    // AuthorityWeight && PageRank
    // if (m.getPageRank() * (2*(m.getAuthorityWeight() + m.getHubWeight()))
    // == this.getPageRank() * (2*(this.getAuthorityWeight() +
    // this.getHubWeight()))) {
    // return 0;
    // } else if (m.getPageRank() * (2*(m.getAuthorityWeight() +
    // m.getHubWeight())) > this.getPageRank() *
    // (2*(this.getAuthorityWeight() + this.getHubWeight()))) {
    // return 1;
    // } else {
    // return -1;
    // }
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
