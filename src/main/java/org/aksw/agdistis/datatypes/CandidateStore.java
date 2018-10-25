package org.aksw.agdistis.datatypes;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author omkar
 * Candiate store that groups candidates based on a key.
 * Key is concat string of anchorIds;
 * 
 * eg: "IBM" and "big blue" may end up in the same store as they return same set of anchorDocs.
 */

public class CandidateStore {
    
    /**
     * Following anchor text is variable. It will be assigned with high frequency candidate.
     */
    private String candidateAnchorText;
    
    private String key;
    
    private List<AnchorDocument> anchorDocuments;
    private List<NamedEntityInText> namedEntities;
    
    
    
    private AnchorDocument bestDocument;
    
    private List<Long> indexes;
    
    public CandidateStore(){};
    
    public CandidateStore(String anchorText,
            List<AnchorDocument> anchorDocuments) {
        super();
        this.candidateAnchorText = anchorText;
        this.anchorDocuments = anchorDocuments;
        this.namedEntities = new ArrayList<NamedEntityInText>();
        indexes = new ArrayList<Long>();
        reInititalizeAnchorDocs();
    }
    
    private void reInititalizeAnchorDocs() {
            /**
             * Documents from cache still carry old metrics
             * re-initialize
             */
        for (AnchorDocument anchorDocument : anchorDocuments) {
            anchorDocument.reinitialize();
        }
    }
    
    public AnchorDocument getBestDocument() {
        return bestDocument;
    }

    public void setBestDocument(AnchorDocument bestDocument) {
        this.bestDocument = bestDocument;
    }
    
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
    
    public String getCandidateAnchorText() {
        return candidateAnchorText;
    }

    public void setCandidateAnchorText(String candidateAnchorText) {
        this.candidateAnchorText = candidateAnchorText;
    }

    public List<Long> getIndexes() {
        return indexes;
    }
    
    public List<AnchorDocument> getAnchorDocuments() {
        return anchorDocuments;
    }
    public void setAnchorDocuments(List<AnchorDocument> anchorDocuments) {
        this.anchorDocuments = anchorDocuments;
    }
    public List<NamedEntityInText> getNamedEntities() {
        return namedEntities;
    }
    public void setNamedEntities(List<NamedEntityInText> namedEntities) {
        this.namedEntities = namedEntities;
    }
    

}
