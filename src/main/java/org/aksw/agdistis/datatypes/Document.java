package org.aksw.agdistis.datatypes;

import java.util.Optional;

public class Document {

  private static final long serialVersionUID = -3213426637730517409L;

  protected Optional<String> documentId;
  private String text;
  private NamedEntitiesInText nes;
  private long disambiguationTime;
  private long candidateSelectionTime;
  private String agdistisVersion;

  public Document() {
  }

  public Document(final String documentId) {
    this.documentId = Optional.of(documentId);
  }

  public Optional<String> getDocumentId() {
    return documentId;
  }

  public void setDocumentId(final String documentId) {
    this.documentId = Optional.of(documentId);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Document) {
      return documentId == ((Document) obj).getDocumentId();
    } else {
      return super.equals(obj);
    }
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append("Document id=" + documentId);
    result.append("\n[ ");
    result.append("]\n");
    return result.toString();
  }

  public void addText(final String text) {
    this.text = text;
  }

  public NamedEntitiesInText getNamedEntitiesInText() {
    return nes;
  }

  public String getText() {
    return text;
  }

  public void addNamedEntitiesInText(final NamedEntitiesInText nes) {
    this.nes = nes;
  }

  /**
   * @return the disambiguationTime
   */
  public long getDisambiguationTime() {
    return disambiguationTime;
  }

  /**
   * @param disambiguationTime the disambiguationTime to set
   */
  public void setDisambiguationTime(final long disambiguationTime) {
    this.disambiguationTime = disambiguationTime;
  }

  /**
   * @return the candidateSelectionTime
   */
  public long getCandidateSelectionTime() {
    return candidateSelectionTime;
  }

  /**
   * @param candidateSelectionTime the candidateSelectionTime to set
   */
  public void setCandidateSelectionTime(final long candidateSelectionTime) {
    this.candidateSelectionTime = candidateSelectionTime;
  }

  /**
   * @return the agdistisVersion
   */
  public String getAgdistisVersion() {
    return agdistisVersion;
  }

  /**
   * @param agdistisVersion the agdistisVersion to set
   */
  public void setAGDISTISVersion(final String agdistisVersion) {
    this.agdistisVersion = agdistisVersion;
  }

}