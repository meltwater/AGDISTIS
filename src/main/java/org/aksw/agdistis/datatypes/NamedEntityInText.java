package org.aksw.agdistis.datatypes;

import java.io.Serializable;

public class NamedEntityInText implements Comparable<NamedEntityInText>, Cloneable, Serializable {

  private static final long serialVersionUID = -6037260139634126022L;

  private int startPos;
  private int length;
  private String namedEntityUri;
  private String label;
  private String type;

  private String domain;

  public String getDomain() {
    return domain;
  }

  public void setDomain(final String domain) {
    this.domain = domain;

  }

  public NamedEntityInText(final int startPos, final int length, final String namedEntityUri, final String type) {
    this.startPos = startPos;
    this.length = length;
    this.namedEntityUri = namedEntityUri;
    if (namedEntityUri != null) {
      label = extractLabel(namedEntityUri);
    }
    this.type = type;
  }

  public NamedEntityInText(final NamedEntityInText entity) {
    startPos = entity.startPos;
    length = entity.length;
    namedEntityUri = entity.namedEntityUri;
    label = entity.label;
    type = entity.type;
  }

  public NamedEntityInText(final NamedEntityInText entity, final int startPos, final int length) {
    this.startPos = startPos;
    this.length = length;
    namedEntityUri = entity.namedEntityUri;
    label = entity.label;
    type = entity.type;
  }

  public int getStartPos() {
    return startPos;
  }

  public void setStartPos(final int startPos) {
    this.startPos = startPos;
  }

  public int getLength() {
    return length;
  }

  public int getEndPos() {
    return startPos + length;
  }

  public void setLength(final int length) {
    this.length = length;
  }

  public String getNamedEntityUri() {
    return namedEntityUri;
  }

  public void setNamedEntity(final String namedEntityUri) {
    this.namedEntityUri = namedEntityUri;
  }

  public String getLabel() {
    return label;
  }

  public String getSingleWordLabel() {
    return label.replaceAll("[ _\\(\\)]", "");
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;

  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append("(\"");
    result.append(namedEntityUri);
    result.append("\", ");
    result.append(startPos);
    result.append(", ");
    result.append(length);
    result.append(", ");
    result.append(type);
    result.append(")");
    return result.toString();
  }

  private static String extractLabel(final String namedEntityUri) {
    final int posSlash = namedEntityUri.lastIndexOf('/');
    final int posPoints = namedEntityUri.lastIndexOf(':');
    if (posSlash > posPoints) {
      return namedEntityUri.substring(posSlash + 1);
    } else if (posPoints < posSlash) {
      return namedEntityUri.substring(posPoints + 1);
    } else {
      return namedEntityUri;
    }
  }

  @Override
  public int compareTo(final NamedEntityInText ne) {
    // NamedEntityInText objects are typically ordered from the end of the
    // text to its beginning
    int diff = (ne.startPos + ne.length) - (startPos + length);
    if (diff == 0) {
      diff = ne.length - length;
      if (diff == 0) {
        diff = namedEntityUri.compareTo(ne.namedEntityUri);
      }
    }
    if (diff < 0) {
      return -1;
    } else if (diff > 0) {
      return 1;
    } else {
      return 0;
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof NamedEntityInText) {
      return compareTo((NamedEntityInText) obj) == 0;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return label.hashCode() ^ startPos ^ length;
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return new NamedEntityInText(this);
  }

}
