package com.meltwater.fhai.kg.ned.agdistis.model;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.io.IOUtils;

/**
 * Created by sonja on 2017-03-10.
 */
public class NEDDocument {

  private Collection<EntityLink> entityLinks;
  private DocumentData documentData;
  private NEDMetadata metadata;

  public NEDDocument() {
    metadata = new NEDMetadata();
    entityLinks = new LinkedList<EntityLink>();
    documentData = new DocumentData();
  }

  public NEDDocument(final NEDMetadata metadata, final DocumentData documentData,
      final Collection<EntityLink> entities) {
    this.metadata = metadata;
    this.documentData = documentData;
    entityLinks = entities;
  }

  /**
   * @return the documentData
   */
  public DocumentData getDocumentData() {
    return documentData;
  }

  /**
   * @return the metadata
   */
  public NEDMetadata getMetadata() {
    return metadata;
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
    result = (prime * result) + ((documentData == null) ? 0 : documentData.hashCode());
    result = (prime * result) + ((entityLinks == null) ? 0 : entityLinks.hashCode());
    result = (prime * result) + ((metadata == null) ? 0 : metadata.hashCode());
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
    if (!(obj instanceof NEDDocument)) {
      return false;
    }
    final NEDDocument other = (NEDDocument) obj;
    if (documentData == null) {
      if (other.documentData != null) {
        return false;
      }
    } else if (!documentData.equals(other.documentData)) {
      return false;
    }
    if (entityLinks == null) {
      if (other.entityLinks != null) {
        return false;
      }
    } else if (!entityLinks.equals(other.entityLinks)) {
      return false;
    }
    if (metadata == null) {
      if (other.metadata != null) {
        return false;
      }
    } else if (!metadata.equals(other.metadata)) {
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
    builder.append("NEDDocument [");
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Entities: ");
    builder.append(entityLinks);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append(documentData);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append(metadata);
    builder.append("]");
    return builder.toString();
  }

  /**
   * @return the entityLinks
   */
  public Collection<EntityLink> getEntityLinks() {
    return entityLinks;
  }

  /**
   * @param entityLinks the entityLinks to set
   */
  public void setEntityLinks(final Collection<EntityLink> entityLinks) {
    this.entityLinks = entityLinks;
  }

  /**
   * @param documentData the documentData to set
   */
  public void setDocumentData(final DocumentData documentData) {
    this.documentData = documentData;
  }

  /**
   * @param metadata the metadata to set
   */
  public void setMetadata(final NEDMetadata metadata) {
    this.metadata = metadata;
  }
}