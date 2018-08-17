package org.aksw.agdistis.datatypes;



public class AnchorDocument implements Comparable<AnchorDocument>{

    public int id;
    public double anchorProb;
    public double score;
    
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }


    public String subject, predicate, object;
    
    public AnchorDocument(final String subject, final String predicate, final String object) {
        id = -1;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }
    
    public AnchorDocument(final int id, final String subject, final String predicate, final String object, final double anchorProb){
        this.id = id;
        this.anchorProb = anchorProb;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }
    
    @Override
    public int hashCode() {
      return id + subject.hashCode();
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
    public int compareTo(AnchorDocument other) {
      if (score > other.score) {
        return -1;
      }
      if (score < other.score) {
        return 1;
      }
      return id - other.id;
    }
    
    
    @Override
    public String toString() {
      return "AnchorDocument{" + "subject='" + subject + '\''+ ", Id="
          + id + ", senseProbabilityStatic=" + anchorProb
          + ", score=" + score + '}';
    }
    

}
