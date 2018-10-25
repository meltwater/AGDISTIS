package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.aksw.agdistis.datatypes.AnchorDocument;
import org.aksw.agdistis.datatypes.CandidateStore;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.CandidateSearcher;
import org.aksw.agdistis.util.Relatedness;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class Disambiguator {

    private static String resourceURI = "http://dbpedia.org/resource/";
    private static final String projectName = "fhai";
    
    private final Logger LOGGER = LoggerFactory
            .getLogger(Disambiguator.class);
    /**
     * THRES_RANK is to pick only those document that have high links to other documents
     */
    private final static double THRES_RANK = 0.20;
    
    /**
     * TOPDOCS_THRES is to decide if we want to consider disambiguating a candidate.
     */
    private final static double TOPDOCS_THRES = 0.05;
    
    private final static double TOPLINKEDDOCS_THRES = 0.10;
    
    private final static double ANCHOR_SHARE = 0.25;
    private final static double LINK_SHARE = 0.75;
    
    private final static double toConsider = 0.10;
    
    private final static int minDocsToConsider = 5;
    
    CandidateSearcher searcher;
    
    
    public Disambiguator() {
        try {
            searcher = new CandidateSearcher();
        } catch (IOException e) {
            LOGGER.error("problem initializing the searcher, check in the index.");
        }
    }
    
    public CandidateSearcher getCandidateSearcher() {
        return searcher;
    }
    
    
    
    public List<CandidateStore> resolve(Collection<CandidateStore> candidates){
        
        Relatedness relatedNess = new Relatedness();
        List<AnchorDocument> allCandidateAnchorDocs = new ArrayList<AnchorDocument>();
        for(CandidateStore candidate1: candidates){
            
            List<AnchorDocument> candidateDocuments  = candidate1.getAnchorDocuments();
            double topIndex = (double)candidate1.getAnchorDocuments().size() * toConsider;
            topIndex = Math.max(minDocsToConsider, topIndex);
            if(topIndex > 0 && candidate1.getNamedEntities().isEmpty()){
                candidateDocuments = candidate1.getAnchorDocuments().subList(0, Math.min(candidate1.getAnchorDocuments().size() - 1, (int)topIndex));
            }
            
            for(AnchorDocument anchorDocument1: candidateDocuments){
                
                
                double relatedEntitySum = 0;
                double mentionSum = 0;
                
                double noOfEntities = 0;
                double noOfMentions = 0;
                boolean isDocumentPresent = false;
                for(CandidateStore candidate2: candidates){
                   
                    if(candidate1.getKey().equals(candidate2.getKey())){
                        continue;
                    }
                    noOfMentions++;
                    List<AnchorDocument> candidateDocuments2  = candidate2.getAnchorDocuments();
                    double topIndex2 = (double)candidate2.getAnchorDocuments().size() * toConsider;
                    topIndex2 = Math.max(minDocsToConsider, topIndex2);
                    if(topIndex2 > 0 && candidate2.getNamedEntities().isEmpty()){
                        candidateDocuments2 = candidate2.getAnchorDocuments().subList(0, Math.min(candidate2.getAnchorDocuments().size() - 1, (int)topIndex2));
                    }
                    for(AnchorDocument anchorDocument2: candidateDocuments2){
                      
                        noOfEntities++;
                        
                        if(anchorDocument1.id == anchorDocument2.id){
                            isDocumentPresent = true;
                        }
                        
                        /**
                         * Entity - Entity relationships
                         * Calculate relatedness and page rank scores
                         * 
                         */
                        double relatedNessScore = relatedNess.getRelatedness(anchorDocument1, anchorDocument2);
                        if(relatedNessScore > 0){
                            relatedEntitySum++;
                            anchorDocument1.linkedDocuments.add(anchorDocument2);
                        }
                        
                        
                    }
                    if(isDocumentPresent){
                        mentionSum++;
                    }
                   
                }
                
                Double mentionProb = (1.0/(1.0 + Math.exp( - (mentionSum/noOfMentions))));
                Double relatedEntityProb = relatedEntitySum/noOfEntities;
                mentionProb = mentionProb.isNaN() ? 0d : mentionProb;
                relatedEntityProb = relatedEntityProb.isNaN() ? 0d : relatedEntityProb;
                anchorDocument1.setMentionProbStatic(mentionProb);
                anchorDocument1.setAverageLinkShareStatic(relatedEntityProb);
                allCandidateAnchorDocs.add(anchorDocument1);
            }
        }
        LOGGER.debug("metrics calculation finished ...");
        
        /**
         *  sort all the anchor Documents and treat top few as relevant candidates that agree with central content of the document
         *  
         */
        allCandidateAnchorDocs.sort(Comparator.comparingDouble(AnchorDocument::getAverageLinkShareStatic).reversed());
        
        double topIndex = (double)allCandidateAnchorDocs.size() * THRES_RANK;
        topIndex = Math.max(minDocsToConsider, topIndex);
        List<AnchorDocument> topDocuments = allCandidateAnchorDocs.subList(0, Math.min(allCandidateAnchorDocs.size() - 1, (int)topIndex));
        List<CandidateStore> resultSet = new ArrayList<CandidateStore>();
        for(CandidateStore candidate:candidates){
            if(candidate.getNamedEntities().isEmpty())continue; // these are context level terms
            if(topDocuments == null || topDocuments.isEmpty())continue; // no top documents to rank;
            
            AnchorDocument bestDocument = null;
            double maxLinkShareScore = 0d;
            
            double topDocumentsMatch = 0d;
            for(AnchorDocument anchorDocument: candidate.getAnchorDocuments()){
                
                /**
                 * Ignore if a document is not linked to any other document.
                 */
                if(anchorDocument.getAverageLinkShareStatic() == 0)continue; 
                
                double linkShareScore = anchorDocument.getAverageLinkShareStatic() + anchorDocument.getMentionProbStatic();
                if(linkShareScore > maxLinkShareScore){
                    maxLinkShareScore = linkShareScore;
                    bestDocument = anchorDocument;
                }
                if(topDocuments.contains(anchorDocument)){
                    topDocumentsMatch++;
                }
            }
            
            double topDocumentShare = (topDocumentsMatch/(double)candidate.getAnchorDocuments().size());
            
            if(topDocumentShare < TOPDOCS_THRES || bestDocument == null)continue;
            
            
            /**
             *  Calculate the link weight of best document
             *  no of top documents it has linked it.
             */
            
            
            double topDocumentsMatchOnBestDocument = 0d;
            for(AnchorDocument linkedDocument: bestDocument.linkedDocuments){
                if(topDocuments.contains(linkedDocument)){
                    topDocumentsMatchOnBestDocument++;
                }
            }
            
            double topDocumentOnBestDocumentShare = topDocumentsMatchOnBestDocument/(double)bestDocument.linkedDocuments.size();
            if(topDocumentOnBestDocumentShare < TOPLINKEDDOCS_THRES)continue;
            bestDocument.setAverageLinkShareStatic(topDocumentOnBestDocumentShare);
            
            candidate.setBestDocument(bestDocument);
            
            double finalScore = ANCHOR_SHARE * bestDocument.getAnchorProb() + LINK_SHARE * bestDocument.getAverageLinkShareStatic();
            bestDocument.setScore(finalScore);
            
            for(NamedEntityInText originalEntity :candidate.getNamedEntities()){
                final String candidateURI = bestDocument.subject;
                String canonicalName = extractLabel(bestDocument);
                if (canonicalName == null) {
                    canonicalName = "";
                }
                originalEntity.setCanonicalName(canonicalName);
                originalEntity.setNamedEntity(candidateURI);
                originalEntity.setAuthorityWeight(bestDocument.getScore());
                List<String> types = Lists.newArrayList();
                for(String type:StringUtils.split(bestDocument.getIdTypeString(),' ')){
                    //TODO get rid of hardcoding
                    if(type.equals("WIKI")){
                        types.add("WIKIPEDIA");
                    }else {
                        types.add(type);
                    }
                }
                originalEntity.setDisambiguatedTypes(types);
            }
            
            resultSet.add(candidate);
        }
        /* Used for debug
        for(CandidateStore result: resultSet){
            if(result.getBestDocument() == null)continue;
            System.out.println(result.getCandidateAnchorText() + " --> "+result.getBestDocument());
        }
        */
       return resultSet; 

    }
    
    private String extractLabel(AnchorDocument document) {

        int resource = document.subject.indexOf(resourceURI);
        if (resource == -1) {
            resource = document.subject.lastIndexOf('/') + 1;
        } else {
            resource = resource + resourceURI.length();
        }

        String label = document.subject.substring(resource);
        if (label.startsWith(projectName)) {
            label = document.object;
        }
        return normaliseTitle(label);
    }

    public static String normaliseTitle(String title) {
        if (title.length() == 0)
            return null;
        if (title.contains("\n"))
            return null; // some parsing error

        StringBuffer s = new StringBuffer();
        s.append(Character.toUpperCase(title.charAt(0)));
        s.append(title.substring(1).replace('_', ' '));

        return s.toString();
    }
}
