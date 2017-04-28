package org.aksw.agdistis.model;

import java.util.Arrays;

public class NamedEntity implements Comparable<NamedEntity> {

  private String namedEntity;
  private int start[];
  private int end[];
  private int offset = -1;
  private String disambiguatedURL;
  private String type;

  public NamedEntity() {

  }

  public NamedEntity(final NamedEntity n) {
    namedEntity = n.getNamedEntity();
    start = n.getStart();
    end = n.getEnd();
    offset = n.getOffset();
    disambiguatedURL = n.getDisambiguatedURL();
    type = n.getType();
  }

  public NamedEntity(final int newStartPost[], final int end[], final int length, final String entityLabel,
      final String type) {
    namedEntity = entityLabel;
    start = newStartPost;
    this.end = end;
    offset = length;
    disambiguatedURL = null;
    this.type = type;
  }

  public String getNamedEntity() {
    return namedEntity;
  }

  public void setNamedEntity(final String namedEntity) {
    this.namedEntity = namedEntity;
  }

  public int[] getStart() {
    return start;
  }

  public void setStart(final int[] start) {
    this.start = start;
  }

  public int[] getEnd() {
    return end;
  }

  public String getType() {
    return type;
  }

  public void setEnd(final int[] end) {
    this.end = end;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(final int offset) {
    this.offset = offset;
  }

  public String getDisambiguatedURL() {
    return disambiguatedURL;
  }

  public void setDisambiguatedURL(final String disambiguatedURL) {
    this.disambiguatedURL = disambiguatedURL;
  }

  public void setType(final String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("NamedEntity [namedEntity=");
    builder.append(namedEntity);
    builder.append(", start=");
    builder.append(Arrays.toString(start));
    builder.append(", end=");
    builder.append(Arrays.toString(end));
    builder.append(", offset=");
    builder.append(offset);
    builder.append(", disambiguatedURL=");
    builder.append(disambiguatedURL);
    builder.append(", type=");
    builder.append(type);
    builder.append("]");
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + ((disambiguatedURL == null) ? 0 : disambiguatedURL.hashCode());
    result = (prime * result) + Arrays.hashCode(end);
    result = (prime * result) + ((namedEntity == null) ? 0 : namedEntity.hashCode());
    result = (prime * result) + offset;
    result = (prime * result) + Arrays.hashCode(start);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NamedEntity other = (NamedEntity) obj;
    if (disambiguatedURL == null) {
      if (other.disambiguatedURL != null) {
        return false;
      }
    } else if (!disambiguatedURL.equals(other.disambiguatedURL)) {
      return false;
    }
    if (!Arrays.equals(end, other.end)) {
      return false;
    }
    if (namedEntity == null) {
      if (other.namedEntity != null) {
        return false;
      }
    } else if (!namedEntity.equals(other.namedEntity)) {
      return false;
    }

    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }

    if (offset != other.offset) {
      return false;
    }
    if (!Arrays.equals(start, other.start)) {
      return false;
    }

    return true;
  }

  @Override
  public int compareTo(final NamedEntity o) {
    if (start[0] < o.start[0]) {
      return -1;
    } else if (start[0] > o.start[0]) {
      return 1;
    }
    return 0;
  }

}
