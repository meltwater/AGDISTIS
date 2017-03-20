package org.aksw.agdistis.util;

import org.apache.commons.lang3.StringUtils;

public class Triple implements Comparable<Triple> {
  public Triple(final String subject, final String predicate, final String object) {
    super();
    this.subject = subject;
    this.predicate = predicate;
    this.object = object;
  }

  String subject;
  String predicate;
  String object;

  public String getSubject() {
    return subject;
  }

  public void setSubject(final String subject) {
    this.subject = subject;
  }

  public String getPredicate() {
    return predicate;
  }

  public void setPredicate(final String predicate) {
    this.predicate = predicate;
  }

  public String getObject() {
    return object;
  }

  public void setObject(final String object) {
    this.object = object;
  }

  @Override
  public String toString() {
    return StringUtils.join(new String[] { subject, predicate, object }, " ");
  }

  @Override
  public int compareTo(final Triple o) {
    final double value = Double.parseDouble(object);
    return new Double(o.getObject()).compareTo(value);
  }

}
