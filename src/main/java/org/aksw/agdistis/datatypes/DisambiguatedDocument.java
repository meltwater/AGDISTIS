package org.aksw.agdistis.datatypes;



public class DisambiguatedDocument implements Comparable<DisambiguatedDocument>{

    
    public double getSenseScoreContext() {
        return senseScoreContext;
    }


    public void setSenseScoreContext(double senseScoreContext) {
        this.senseScoreContext = senseScoreContext;
    }


    public double getScore() {
        return score;
    }


    public void setScore(double score) {
        this.score = score;
    }


    public String getAnchor() {
        return anchor;
    }


    public int getCountInText() {
        return countInText;
    }


    public int getConceptId() {
        return conceptId;
    }


    public String getConceptName() {
        return conceptName;
    }


    public double getSenseProbabilityStatic() {
        return senseProbabilityStatic;
    }

    private final String anchor;
    private final int countInText;
    private final int conceptId;
    private final String conceptName;
    private final double senseProbabilityStatic;
    private double senseScoreContext;
    // can change dynamically from outside before sorting
    private double score;

    
    public DisambiguatedDocument(String anchor, int countInText, int id,
            String subject, double anchorProb, double anchorProb2) {
        this.anchor = anchor;
        this.countInText = countInText;
        this.conceptId = id;
        this.conceptName = subject;
        this.senseProbabilityStatic = anchorProb;
        this.senseScoreContext = anchorProb2;
    }
    
    
    @Override
    public int hashCode() {
      return conceptId + anchor.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if ((other == null) || (getClass() != other.getClass())) {
        return false;
      }
      return hashCode() == other.hashCode();
    }

    @Override
    public int compareTo(DisambiguatedDocument other) {
      if (score > other.score) {
        return -1;
      }
      if (score < other.score) {
        return 1;
      }
      return conceptId - other.conceptId;
    }

    @Override
    public String toString() {
      return "WikipediaConcept{" + "anchor='" + anchor + '\'' + ", countInText=" + countInText + ", conceptId="
          + conceptId + ", conceptName='" + conceptName + '\'' + ", senseProbabilityStatic=" + senseProbabilityStatic
          + ", senseScoreContext=" + senseScoreContext + ", score=" + score + '}';
    }

}
