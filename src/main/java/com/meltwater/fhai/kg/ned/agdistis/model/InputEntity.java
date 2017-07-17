package com.meltwater.fhai.kg.ned.agdistis.model;

/**
 * Created by sonja on 2017-03-22.
 */
public class InputEntity {

  private final String name;
  private final String type;
  private final int startOffset;
  private final int endOffset;

  public InputEntity(final String name, final String type, final int startOffset, final int endOffset) {
    this.name = name;
    this.type = type;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public int getStartOffset() {
    return startOffset;
  }

  public int getEndOffset() {
    return endOffset;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + endOffset;
    result = (prime * result) + ((name == null) ? 0 : name.hashCode());
    result = (prime * result) + startOffset;
    result = (prime * result) + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof InputEntity)) {
      return false;
    }
    final InputEntity other = (InputEntity) obj;
    if (endOffset != other.endOffset) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (startOffset != other.startOffset) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(name);
    builder.append(" [");
    builder.append(type);
    builder.append("]=<");
    builder.append(startOffset);
    builder.append(",");
    builder.append(endOffset);
    builder.append(">");
    return builder.toString();
  }

}