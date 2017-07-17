package com.meltwater.fhai.kg.ned.agdistis.model;

import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 *
 */
public class EntityLink {
  private String uuid;
  private String dbpediaRef;
  private String canonicalName;
  private Collection<String> nedTypes;
  private String nerType;
  private String surfaceForm;
  private List<Occurrence> occurrences;
  private double popularityScore;
  private double disambiguationScore;

  /**
   * @return the uuid
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * @param uuid the uuid to set
   */
  public void setUuid(final String uuid) {
    this.uuid = uuid;
  }

  /**
   * @return the dbpediaRef
   */
  public String getDbpediaRef() {
    return dbpediaRef;
  }

  /**
   * @param dbpediaRef the dbpediaRef to set
   */
  public void setDbpediaRef(final String dbpediaRef) {
    this.dbpediaRef = dbpediaRef;
  }

  /**
   * @return the canonicalName
   */
  public String getCanonicalName() {
    return canonicalName;
  }

  /**
   * @param canonicalName the canonicalName to set
   */
  public void setCanonicalName(final String canonicalName) {
    this.canonicalName = canonicalName;
  }

  /**
   * @return the nedTypes
   */
  public Collection<String> getNedTypes() {
    return nedTypes;
  }

  /**
   * @param nedTypes the nedTypes to set
   */
  public void setNedTypes(final Collection<String> nedTypes) {
    this.nedTypes = nedTypes;
  }

  /**
   * @return the nerType
   */
  public String getNerType() {
    return nerType;
  }

  /**
   * @param nerType the NER type to set
   */
  public void setNerType(final String nerType) {
    this.nerType = nerType;
  }

  /**
   * @return the surfaceForm
   */
  public String getSurfaceForm() {
    return surfaceForm;
  }

  /**
   * @param surfaceForm the surfaceForm to set
   */
  public void setSurfaceForm(final String surfaceForm) {
    this.surfaceForm = surfaceForm;
  }

  /**
   * @return the occurrences
   */
  public List<Occurrence> getOccurrences() {
    return occurrences;
  }

  /**
   * @param occurrences the occurrences to set
   */
  public void setOccurrences(final List<Occurrence> occurrences) {
    this.occurrences = occurrences;
  }

  /**
   * @return the popularityScore
   */
  public double getPopularityScore() {
    return popularityScore;
  }

  /**
   * @param popularityScore the popularityScore to set
   */
  public void setPopularityScore(final double popularityScore) {
    this.popularityScore = popularityScore;
  }

  /**
   * @return the disambiguationScore
   */
  public double getDisambiguationScore() {
    return disambiguationScore;
  }

  /**
   * @param disambiguationScore the disambiguationScore to set
   */
  public void setDisambiguationScore(final double disambiguationScore) {
    this.disambiguationScore = disambiguationScore;
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
    result = (prime * result) + ((canonicalName == null) ? 0 : canonicalName.hashCode());
    result = (prime * result) + ((dbpediaRef == null) ? 0 : dbpediaRef.hashCode());
    long temp;
    temp = Double.doubleToLongBits(disambiguationScore);
    result = (prime * result) + (int) (temp ^ (temp >>> 32));
    result = (prime * result) + ((nedTypes == null) ? 0 : nedTypes.hashCode());
    result = (prime * result) + ((nerType == null) ? 0 : nerType.hashCode());
    result = (prime * result) + ((occurrences == null) ? 0 : occurrences.hashCode());
    temp = Double.doubleToLongBits(popularityScore);
    result = (prime * result) + (int) (temp ^ (temp >>> 32));
    result = (prime * result) + ((surfaceForm == null) ? 0 : surfaceForm.hashCode());
    result = (prime * result) + ((uuid == null) ? 0 : uuid.hashCode());
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
    if (!(obj instanceof EntityLink)) {
      return false;
    }
    final EntityLink other = (EntityLink) obj;
    if (canonicalName == null) {
      if (other.canonicalName != null) {
        return false;
      }
    } else if (!canonicalName.equals(other.canonicalName)) {
      return false;
    }
    if (dbpediaRef == null) {
      if (other.dbpediaRef != null) {
        return false;
      }
    } else if (!dbpediaRef.equals(other.dbpediaRef)) {
      return false;
    }
    if (Double.doubleToLongBits(disambiguationScore) != Double.doubleToLongBits(other.disambiguationScore)) {
      return false;
    }
    if (nedTypes == null) {
      if (other.nedTypes != null) {
        return false;
      }
    } else if (!nedTypes.equals(other.nedTypes)) {
      return false;
    }
    if (nerType == null) {
      if (other.nerType != null) {
        return false;
      }
    } else if (!nerType.equals(other.nerType)) {
      return false;
    }
    if (occurrences == null) {
      if (other.occurrences != null) {
        return false;
      }
    } else if (!occurrences.equals(other.occurrences)) {
      return false;
    }
    if (Double.doubleToLongBits(popularityScore) != Double.doubleToLongBits(other.popularityScore)) {
      return false;
    }
    if (surfaceForm == null) {
      if (other.surfaceForm != null) {
        return false;
      }
    } else if (!surfaceForm.equals(other.surfaceForm)) {
      return false;
    }
    if (uuid == null) {
      if (other.uuid != null) {
        return false;
      }
    } else if (!uuid.equals(other.uuid)) {
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
    builder.append("EntityLink [");
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("UUID: ");
    builder.append(uuid);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("DBPedia Reference: ");
    builder.append(dbpediaRef);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Canonical Name: ");
    builder.append(canonicalName);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("NED Types: ");
    builder.append(nedTypes);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("NER Type=");
    builder.append(nerType);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Surface Form: ");
    builder.append(surfaceForm);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Occurrences: ");
    builder.append(occurrences);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Popularity Score: ");
    builder.append(popularityScore);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Disambiguation Score: ");
    builder.append(disambiguationScore);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("]");
    return builder.toString();
  }
}