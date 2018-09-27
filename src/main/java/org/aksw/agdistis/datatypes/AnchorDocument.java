package org.aksw.agdistis.datatypes;

import java.util.List;



public class AnchorDocument implements Comparable<AnchorDocument>{

    public int id;
    private String idTypeString;
    

    private double anchorProb;
    private double linkShare;
    private double pageRank;
    
    private double score;
    
    public List<Integer> inLinks;
    
    public int linkedMentionIndex;

    public String subject, predicate, 
    object; // Object will be either anchor text or URI of another entity.
    public double linkWeight;

    public boolean isTaboo;

    private double mentionProb;
    private double pageRankDistance;
    private double averageLinkShare; 
    
    private double mentionProbStatic;
    private double pageRankDistanceStatic;
    private double averageLinkShareStatic;
    
    

    public AnchorDocument(final String subject, final String predicate, final String object) {
        id = -1;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }
    
    public AnchorDocument(final int id, final String idTypeString, final String subject, final String predicate, final String object, final double anchorProb, final double pageRank){
        this.id = id;
        this.idTypeString = idTypeString;
        this.anchorProb = anchorProb;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.pageRank = pageRank;
    }
    
    public void reinitialize() {
        this.linkShare = 0d;
        this.score = 0d;
        this.inLinks = null;
        this.linkedMentionIndex = -1;
        this.linkWeight = 0d;
        this.isTaboo = false;
        
        this.mentionProb = 0d;
        this.mentionProbStatic = 0d;
        this.averageLinkShare = 0d;
        this.averageLinkShareStatic = 0d;
        this.pageRankDistance = 0d;
        this.pageRankDistanceStatic = 0d;
        
    }
    
    public String getIdTypeString() {
        return idTypeString;
    }

    public void setIdTypeString(String idTypeString) {
        this.idTypeString = idTypeString;
    }
    
    public double getMentionProbStatic() {
        return mentionProbStatic;
    }

    public void setMentionProbStatic(double mentionProbStatic) {
        this.mentionProbStatic = mentionProbStatic;
    }

    public double getPageRankDistanceStatic() {
        return pageRankDistanceStatic;
    }

    public void setPageRankDistanceStatic(double pageRankDistanceStatic) {
        this.pageRankDistanceStatic = pageRankDistanceStatic;
    }

    public double getAverageLinkShareStatic() {
        return averageLinkShareStatic;
    }

    public void setAverageLinkShareStatic(double averageLinkShareStatic) {
        this.averageLinkShareStatic = averageLinkShareStatic;
    }
    
    
    public double getAverageLinkShare() {
        return averageLinkShare;
    }

    public void setAverageLinkShare(double averageLinkShare) {
        this.averageLinkShare = averageLinkShare;
    }

    public double getPageRankDistance() {
        return pageRankDistance;
    }

    public void setPageRankDistance(double pageRankDistance) {
        this.pageRankDistance = pageRankDistance;
    }
    
    public void setMentionProb(double mentionProb) {
        this.mentionProb = mentionProb;
    }
    
    public double getMentionProb() {
        return mentionProb;
    }
    
    public String getAnchorText(){
        return this.object;
    }
    
    
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
    
    public double getAnchorProb() {
        return anchorProb;
    }

    public void setAnchorProb(double anchorProb) {
        this.anchorProb = anchorProb;
    }

    public double getLinkShare() {
        return linkShare;
    }

    public void setLinkShare(double linkShare) {
        this.linkShare = linkShare;
    }

    public double getPageRank() {
        return pageRank;
    }

    public void setPageRank(double pageRank) {
        this.pageRank = pageRank;
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
      return "AnchorDocument{" + "anchorText='" + subject + '\''+ ", Id="
          + id + ", Anchor Probabilty= " + anchorProb
          + ", Page Rank= " + pageRankDistance + ", Link Share= "+averageLinkShare+ ", Score= "+score+ '}';
    }

    

    


}
