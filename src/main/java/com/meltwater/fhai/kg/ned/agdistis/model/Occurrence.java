package com.meltwater.fhai.kg.ned.agdistis.model;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by sonja on 2017-03-17.
 */
public class Occurrence implements Comparable<Occurrence> {
  private int start;
  private int end;

  public Occurrence(final int start, final int end) {
    this.start = start;
    this.end = end;
  }

  public Occurrence() { }

  public int getStartOffset() {
    return start;
  }

  public void setStartOffset(final int start) {
    this.start = start;
  }

  public int getEndOffset() {
    return end;
  }

  public void setEndOffset(final int end) {
    this.end = end;
  }

  @Override
  public String toString() {
    return StringUtils.join("{\"start\": ", Integer.toString(start), ", \"end\": ", Integer.toString(end), "}");
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Occurrence)) {
      return false;
    }

    final Occurrence that = (Occurrence) o;

    if (start != that.start) {
      return false;
    }
    return end == that.end;
  }

  @Override
  public int hashCode() {
    int result = start;
    result = (31 * result) + end;
    return result;
  }

  @Override
  public int compareTo(Occurrence o) {
    if (start != o.getStartOffset()) {
      return (start - o.getStartOffset());
    }
    return (end - o.getEndOffset());
  }
}
