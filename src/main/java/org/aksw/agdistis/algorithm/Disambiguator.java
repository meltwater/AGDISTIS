package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.agdistis.datatypes.AnchorDocument;
import org.aksw.agdistis.datatypes.CandidateStore;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.CandidateSearcher;
import org.aksw.agdistis.util.Relatedness;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Disambiguator {
    
    private static String resourceURI = "http://dbpedia.org/resource/";
    private static final String projectName = "fhai";
    
    private final Logger LOGGER = LoggerFactory
            .getLogger(Disambiguator.class);
    
    private CandidateSearcher searcher;
    private CorporationAffixCleaner cleaner;
    
    /**
     * This is when we have lots of candidates in the top
     * More the DISCARD_TOP_DOCS more we get rid of top documents
     */
    private final static double FINAL_SCORE_THRES = 0.20;
    
    /**
     * CAND_TOPDOCS_THRES is to decide if a candidate store should be considered for disambiguation
     * Consider if above the threshold
     */
    private final static double CAND_TOPDOCS_THRES = 0.05;
    
    /**
     * TOPDOCS_THRES is to check how many related documents the best document is linked to
     */
    private final static double TOPDOCS_THRES = 0.20;
    
    private final static double ANCHOR_SHARE = 0.75;
    private final static double LINK_SHARE = 0.25;
    
    public Disambiguator() {
        
        try {
            searcher = new CandidateSearcher();
        } catch (IOException e) {
            LOGGER.error("problem initializing the searcher, check in the index.");
        }
        try {
            cleaner = new CorporationAffixCleaner();
        } catch (IOException e) {
            LOGGER.error("problem initializing the Affix cleaner");
        }
    }
    
    
    public List<CandidateStore> resolve(final Document document){
        
        
        
        final List<AnchorDocument> anchorDocumentStore = new ArrayList<AnchorDocument>();
        final Relatedness relatedNess = new Relatedness();
        final List<CandidateStore> candidates = createCandidateStores(document.getNamedEntitiesInText().getNamedEntities(), anchorDocumentStore);
        
        if(anchorDocumentStore.isEmpty()){
            /**
             * No Documents to proces
             * return.
             */
            return candidates;
        }
        /**
         * Calculate contextual relatedness
         * AnchorDocument V/S All Other AnchorDocument's except from its own store
         */
        double nonZeroDocs  = 0;
        for(CandidateStore candidate1:candidates){
            
            
            for(int i=candidate1.getStoreIndexStart(); i<candidate1.getStoreIndexEnd();i++){
                
                AnchorDocument document1 =  anchorDocumentStore.get(i);
                double relatedEntitySum = 0;
                double mentionSum = 0;
                
                double noOfEntities = 0;
                double noOfMentions = 0;
                boolean isDocumentPresent = false;
                
                for(CandidateStore candidate2: candidates){
                    
                    if(candidate1.getCandidateAnchorText().equals(candidate2.getCandidateAnchorText())){
                        continue;
                    }
                    noOfMentions++;
                    for(int j=candidate2.getStoreIndexStart(); j<candidate2.getStoreIndexEnd();j++){
                        
                        AnchorDocument document2 =  anchorDocumentStore.get(j);
                        noOfEntities++;
                        
                        if(document1.id == document2.id){
                            isDocumentPresent = true;
                        }
                        
                        /**
                         * Entity - Entity relationships
                         * Calculate relatedness and page rank scores
                         * 
                         */
                        double relatedNessScore = relatedNess.getRelatedness(document1, document2);
                        if(relatedNessScore > 0){
                            relatedEntitySum++;
                            document1.relatedDocumentIds.add(j);
                        }
                        
                    }
                    if(isDocumentPresent){
                        mentionSum++;
                    }
                }
                
                /**
                 * Calculate probabilities.
                 */
                double mentionProb = (1.0/(1.0 + Math.exp( - (mentionSum/noOfMentions)))); // sigmoid
                double relatedEntityProb = relatedEntitySum/noOfEntities;
                mentionProb = Double.isNaN(mentionProb) ? 0d : mentionProb;
                relatedEntityProb = Double.isNaN(relatedEntityProb) ? 0d : relatedEntityProb;
                document1.setMentionProbStatic(mentionProb);
                document1.setAverageLinkShareStatic(relatedEntityProb);
                if(relatedEntityProb > 0){
                    nonZeroDocs++;
                }
            }
            
        }
        
        
        /**
         * Rank and sort anchor Document store based on link share only
         */
        
        
        List<AnchorDocument> topDocumentsStore = Lists.newArrayList(anchorDocumentStore);
        topDocumentsStore.sort(Comparator.comparingDouble(AnchorDocument::getAverageLinkShareStatic).reversed());
        
        double anchor_docs_share = ((double)nonZeroDocs/(double)anchorDocumentStore.size());
        int rank_index = (int)Math.round((double)((topDocumentsStore.size() - 1) * (anchor_docs_share)));
        topDocumentsStore = topDocumentsStore.subList(0, rank_index);
        
        /**
         * Finally calculate score of each candidate store
         * Ignore a store if it doesn't contain any top documents
         * 
         */
        
        for(CandidateStore candidate: candidates){
            
            double topDocumentsMatch = 0d;
            double maxScore = 0d;
            AnchorDocument bestDocument = null;
            for(int i=candidate.getStoreIndexStart(); i<candidate.getStoreIndexEnd(); i++){
                
                AnchorDocument doc =  anchorDocumentStore.get(i);
                if(doc.getAverageLinkShareStatic() == 0)continue;
                
                double curScore = doc.getAverageLinkShareStatic() + doc.getMentionProbStatic();
                if(curScore > maxScore){
                    maxScore = curScore;
                    bestDocument = doc;
                }
                if(topDocumentsStore.contains(doc)){
                    topDocumentsMatch++;
                }
            }
            
            if(null == bestDocument)continue;
            
            
            /**
             * Now that we have best document, check if its linked to top documents
             */
            
            double topDocumentsMatch_LD = 0d;
            for(int index:bestDocument.relatedDocumentIds){
                if(topDocumentsStore.contains(anchorDocumentStore.get(index))){
                    topDocumentsMatch_LD++;
                }
            }
            double topDocumentsProb_LD = topDocumentsMatch_LD/((double)bestDocument.relatedDocumentIds.size());
            double topDocumentsProb = topDocumentsMatch/((double)(candidate.getStoreIndexEnd() - candidate.getStoreIndexStart()));
            
            topDocumentsProb_LD = Double.isNaN(topDocumentsProb_LD) ? 0 : topDocumentsProb_LD;
            topDocumentsProb = Double.isNaN(topDocumentsProb) ? 0 : topDocumentsProb;
                   
            if(topDocumentsProb < CAND_TOPDOCS_THRES && topDocumentsProb_LD < TOPDOCS_THRES)continue;
            
            candidate.setBestDocument(bestDocument);
            
            bestDocument.setAverageLinkShareStatic(Math.max(topDocumentsProb_LD, topDocumentsProb));
            
            double finalScore = ANCHOR_SHARE * bestDocument.getAnchorProb() + LINK_SHARE * bestDocument.getAverageLinkShareStatic();
            if(finalScore < FINAL_SCORE_THRES)continue;
            
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
        }
        
        return candidates;
        
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
    
    
    private List<CandidateStore> createCandidateStores(final List<NamedEntityInText> namedEntities, final List<AnchorDocument> anchorDocumentStore){
        Map<String, CandidateStore> candidateStores = new HashMap<String, CandidateStore>();
        
        for(final NamedEntityInText namedEntity: namedEntities){
            String surfaceForm = namedEntity.getSurfaceForm();
            // check if already have a store with the same surfaceform
            /**
             * WARN - Doesn't handle US Open (golf) and US Open (tennis). Both will end up in same store
             * Also, this is very rare case. if this happens then it might be a single document with two different contents.
             */
            // clean surface form
            surfaceForm = cleaner.cleanLabelsfromCorporationIdentifier(surfaceForm);
            
            CandidateStore store = candidateStores.get(surfaceForm);
            
            if(candidateStores.containsKey(surfaceForm)){
                candidateStores.get(surfaceForm).getNamedEntities().add(namedEntity);
                continue;
            }
            
            // actual search to index happens here.
            
            List<AnchorDocument> anchorDocs = searcher.searchAnchorText(surfaceForm);
            if(anchorDocs == null || anchorDocs.isEmpty()){
                anchorDocs = searcher.searchAnchorText(surfaceForm.toLowerCase());
            }
            if(anchorDocs == null || anchorDocs.isEmpty())continue;
            /**
             * Re-initialize docs to avoid wrong calculation coz of cache.
             */
            int startIndex = anchorDocumentStore.size();
            int endIndex = anchorDocumentStore.size();
            for(AnchorDocument d:anchorDocs){
                d.reinitialize();
                anchorDocumentStore.add(d);
                endIndex++;
            }
            store = new CandidateStore(surfaceForm, startIndex, endIndex);
            store.getNamedEntities().add(namedEntity);
            candidateStores.put(surfaceForm, store);
            
        }
        if(candidateStores.values().isEmpty()){
            return Lists.newArrayList();
        }
        return Lists.newArrayList(candidateStores.values());
    }

}
