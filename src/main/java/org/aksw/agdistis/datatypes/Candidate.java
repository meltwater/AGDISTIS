package org.aksw.agdistis.datatypes;

import org.apache.commons.lang3.StringUtils;

public class Candidate implements Comparable<Candidate> {

  public static final int NO_ENTITY_CANDIDATE_ID = -1;
  public static final int OTHER_ENTITY_CANDIDATE_ID = -2;

  private int id;
  private String url;
  private String label;
  private String description;
  private int outgoingEdgeCount;

  public Candidate(final String url, final String label, final String description) {
    this(NO_ENTITY_CANDIDATE_ID, url, label, description);
  }

  public Candidate(final int id, final String url, final String label, final String description) {
    this(id, url, label, description, -1);
  }

  public Candidate(final String url, final String label, final String description, final int outgoingEdgeCount) {
    this(NO_ENTITY_CANDIDATE_ID, url, label, description, outgoingEdgeCount);
  }

  public Candidate(final int id, final String url, final String label, final String description,
      final int outgoingEdgeCount) {
    this.id = id;
    this.url = url;
    this.label = label;
    this.description = description;
    this.outgoingEdgeCount = outgoingEdgeCount;
  }

  public int getId() {
    return id;
  }

  public void setId(final int id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  @Override
  public int hashCode() {
    return url.hashCode();
  }

  @Override
  public boolean equals(final Object c) {
    if (c instanceof Candidate) {
      if (((Candidate) c).getUrl().equals(url)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return StringUtils.join("c: ", label, " -> ", url, " #E ", Integer.toString(outgoingEdgeCount));
  }

  @Override
  public int compareTo(final Candidate o) {
    if (o.getOutgoingEdgeCount() > getOutgoingEdgeCount()) {
      return 1;
    } else if (o.getOutgoingEdgeCount() < getOutgoingEdgeCount()) {
      return -1;
    } else {
      return 0;
    }
  }

  public int getOutgoingEdgeCount() {
    return outgoingEdgeCount;
  }

  public void setOutgoingEdgeCount(final int e) {
    outgoingEdgeCount = e;
  }
}
