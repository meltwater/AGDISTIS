package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.aksw.agdistis.datatypes.AnchorDocument;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.CandidateSearcher;
import org.aksw.agdistis.util.Relatedness;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class GraphResolver {
    
    
    private static String resourceURI = "http://dbpedia.org/resource/";
    private static final String projectName = "fhai";
    
    public static final double ANCHOR_THRES = 0.10;
    public static final double DISAMBIGUATTION_THRES = 0.30;
    // all of the following should add up to 1.0;
    public static final double LINK_SHARE = 0.50;
    public static final double PR_SHARE = 0.25;
    public static final double ANCHOR_SHARE = 0.25;
    
    
    
    private final Logger LOGGER = LoggerFactory
            .getLogger(GraphResolver.class);
    
    CandidateSearcher searcher;
    
    
    
    
    public GraphResolver() {
        
        try {
            searcher = new CandidateSearcher();
        } catch (IOException e) {
            LOGGER.error("problem initializing the searcher, check in the index.");
        }
    }
    
    public void resolve(final Document document){
        

        Relatedness relatednessP = new Relatedness();
        
       /**
        * Search for candidate entities using mentions
        */
        
        
        List<MentionOccurrence> mentionOccurences = collectMentionOccurences(document
                .getNamedEntitiesInText());
        List<AnchorDocument> trustedDocs = collectTrustedDocs(mentionOccurences);
        
        
        double mentionCount = mentionOccurences.size();
       
        double entityCount = trustedDocs.size(); 
        /**
         * Calculate Entity - Mention distance
         */
        
        for(AnchorDocument entity: trustedDocs){
            
            double mentionProbSum = 0d;
            for(MentionOccurrence mention: mentionOccurences){
                
                int foundId = Arrays.binarySearch(mention.docIds, entity.id);
                if(foundId >= 0){
                    // add the probabilty
                    mentionProbSum += entity.getAnchorProb();
                }
            }
            entity.setMentionProbStatic(mentionProbSum);
            entity.setMentionProb(mentionProbSum/mentionCount);
        }
        
        
        /**
         * Calculate Entity - Entity pagerank difference
         */
        
        /**
         * Calculate Entity - Entity link share avg 
         *
         */
        /**
         * Calculate only sums here
         */
        
        for(AnchorDocument entity1 : trustedDocs){
            
            // page rank
            double pageRank = entity1.getPageRank();
            double distanceSum = 0;
            
            // avglinkShare
            double linkShareSum = 0;
            for(AnchorDocument entity2: trustedDocs){
                
               double distance = Math.max(pageRank, entity2.getPageRank()) - Math.min(pageRank, entity2.getPageRank());
               distanceSum += distance;
               
               double linkShare = relatednessP.getRelatedness(entity1, entity2);
               linkShareSum += linkShare;
            }
            
            double pageRankDistanceSum = ((distanceSum > 0) ? distanceSum : 0);
            entity1.setPageRankDistanceStatic(pageRankDistanceSum);
            entity1.setPageRankDistance(1.0 - (pageRankDistanceSum/entityCount));
            
            double angLinkShare = (linkShareSum > 0) ? linkShareSum : 0;
            entity1.setAverageLinkShareStatic(angLinkShare);
            entity1.setAverageLinkShare(angLinkShare/entityCount);
        }
        
        /**
         * Eliminate Entities with less weighted degree, making sure the density of the subgraph is maximum
         */
        
        
        double previousMinWeightedDegree = Double.MAX_VALUE;
        int iterations = 0;
        while(iterations <= entityCount){
            iterations++;
            AnchorDocument entityToRemove = null;
            double mentionToRemoveCnt = 0;
            double entityToRemoveCnt = 0;
            
            double anchorProbToRemove = 0;
            double pageRankDistanceToRemove = 0;
            double avgLinkShareToRemove = 0;
            double minWeightedDegree = Double.MAX_VALUE;
            boolean allTabooRemoved = true;
            for(AnchorDocument entity:trustedDocs){
                
                if(entity.isTaboo)continue;
                if(mentionOccurences.get(entity.linkedMentionIndex).isTaboo)continue;
                allTabooRemoved = false;
                double currentMentionShareCount = (mentionCount - mentionToRemoveCnt);
                double currentEntityCount = (entityCount  - entityToRemoveCnt);
                
                if(currentMentionShareCount == 0 || currentEntityCount == 0){
                    allTabooRemoved = true;
                    break;
                }
                double mentionProbShare = (entity.getMentionProbStatic() - anchorProbToRemove)/currentMentionShareCount ;
                
                double avgLinkShare = (entity.getAverageLinkShareStatic() - avgLinkShareToRemove)/currentEntityCount;
                
                double pageRankDistanceShare = 1.0 - ((entity.getPageRankDistanceStatic() - pageRankDistanceToRemove)/currentEntityCount);
                
                double weightedDegree = mentionProbShare + avgLinkShare + pageRankDistanceShare;
                entity.setMentionProb(mentionProbShare);
                entity.setAverageLinkShare(avgLinkShare);
                entity.setPageRankDistance(pageRankDistanceShare);
                if(weightedDegree < minWeightedDegree){
                    minWeightedDegree = weightedDegree;
                    entityToRemove = entity;
                }
            }
            if(entityToRemove == null)break;
            if(allTabooRemoved)break;
            MentionOccurrence linkedMention = mentionOccurences.get(entityToRemove.linkedMentionIndex);
            if(linkedMention.totalDocs == 1){
                linkedMention.isTaboo = true;
                mentionToRemoveCnt++;
            }else if(linkedMention.totalDocs > 1){
                linkedMention.totalDocs -= 1d;
                entityToRemoveCnt++;
                entityToRemove.isTaboo = true;
            }
            
            // calculate the share of prob to remove
            avgLinkShareToRemove = entityToRemove.getAverageLinkShareStatic()/entityCount;
            anchorProbToRemove = entityToRemove.getAnchorProb();
            pageRankDistanceToRemove = entityToRemove.getPageRankDistanceStatic()/entityCount;
            
            
            // if the weighted degree is increased ? STOP
            if(minWeightedDegree > previousMinWeightedDegree){
                break;
            }
            previousMinWeightedDegree = minWeightedDegree;
        }
       
       chooseTheTop(mentionOccurences);
       
        
     
        
    }
    
    private void chooseTheTop(Collection<MentionOccurrence> mentionOccurences) {

        for (MentionOccurrence mentionOccurence : mentionOccurences) {

            final AnchorDocument topScored = getTopCandidate(mentionOccurence.anchorDocs);
            if (null == topScored) {
                continue;
            }
            final NamedEntityInText originalEntity = mentionOccurence.originalEntity;
            final String candidateURI = topScored.subject;
            String canonicalName = extractLabel(topScored);
            if (canonicalName == null) {
                canonicalName = "";
            }
            originalEntity.setCanonicalName(canonicalName);
            originalEntity.setNamedEntity(candidateURI);
            originalEntity.setAuthorityWeight(topScored.getScore());
            List<String> types = Lists.newArrayList();
            for(String type:StringUtils.split(topScored.getIdTypeString(),' ')){
                //TODO get rid of hardcoding
                if(type.equals("WIKI")){
                    types.add("WIKIPEDIA");
                }else if(type.equals("DBPEDIA")){
                    types.add("DBPEDIA");
                }else if(type.equals("FHAI")){
                    types.add("MELTWATER");
                }
            }
            originalEntity.setDisambiguatedTypes(types);
        }

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
    
    private AnchorDocument getTopCandidate(List<AnchorDocument> anchorDocs) {
        AnchorDocument topDocument = null;
        double topProb = 0;
        for (AnchorDocument anchorDoc : anchorDocs) {
            if(anchorDoc.isTaboo)continue;
            calculateCombinedScore(anchorDoc);
            if (anchorDoc.getScore() > topProb) {
                topProb = anchorDoc.getScore();
                topDocument = anchorDoc;
            }
        }
        // apply threshold here
        if(null != topDocument && topDocument.getScore() >= DISAMBIGUATTION_THRES){
            return topDocument;
        }
        return topDocument;
    }
    
    private void calculateCombinedScore(AnchorDocument doc) {
        double score = (LINK_SHARE * doc.getAverageLinkShare())
                + (PR_SHARE * doc.getPageRankDistance())
                + (ANCHOR_SHARE * doc.getAnchorProb());
        doc.setScore(score);
    }

    private List<AnchorDocument> collectTrustedDocs(
            Collection<MentionOccurrence> mentionOccurences) {
        List<AnchorDocument> trustedDocs = Lists.newArrayList();
        for (MentionOccurrence anchorOccurence : mentionOccurences) {

            /**
             * Filter by high anchor probability
             */
            List<AnchorDocument> anchorDocuments = anchorOccurence.anchorDocs
                    .stream()
                    .filter(senseCandidate -> (senseCandidate.getAnchorProb() > ANCHOR_THRES))
                    .collect(Collectors.toList());
            trustedDocs.addAll(anchorDocuments);
        }
        return trustedDocs;
    }
    
    private List<MentionOccurrence> collectMentionOccurences(
            NamedEntitiesInText namedEntitiesInText) {
        List<MentionOccurrence> mentionOccurences = new ArrayList<GraphResolver.MentionOccurrence>();
        for (NamedEntityInText ne : namedEntitiesInText) {
            
            List<AnchorDocument> anchorDocuments = candidateSearch(ne
                    .getSurfaceForm());
            mentionOccurences.add(new MentionOccurrence(anchorDocuments, ne));
            /**
             * Collect in links ids
             */
            for (AnchorDocument anchorDocument : anchorDocuments) {
                // reinitialize
                anchorDocument.reinitialize();
                anchorDocument.inLinks = searchForInLinks(anchorDocument);
                anchorDocument.linkedMentionIndex = mentionOccurences.size() - 1;
            }
            
        }
        return mentionOccurences;
    }
    
    private List<Integer> searchForInLinks(AnchorDocument anchorDocument) {
        List<Integer> inLinkIds = Lists.newArrayList();
        List<AnchorDocument> inlinkDocs = searcher
                .searchInLinks(anchorDocument.subject);
        if (null == inlinkDocs || inlinkDocs.isEmpty())
            return inLinkIds;
        for (AnchorDocument inLinkDoc : inlinkDocs) {
            inLinkIds.add(inLinkDoc.id);
        }
        // Sorting saves lot of time in relatedness calculation later.
        Collections.sort(inLinkIds);

        return inLinkIds;
    }
    
    private List<AnchorDocument> candidateSearch(String anchor) {

        // String anchorAsObject = "\""+anchor+"\"@en";
        String anchorAsObject = anchor;
        List<AnchorDocument> results = searcher.searchAnchorText(anchorAsObject);
        if (null == results || results.isEmpty()) {
            // lowercased anchor
            anchorAsObject = anchor.toLowerCase();
            results = searcher.searchAnchorText(anchorAsObject);
        }
        if (null == results) {
            results = new ArrayList<AnchorDocument>();
        }
        return results;
    }
    
    private class MentionOccurrence {
        List<AnchorDocument> anchorDocs;
        public int[] docIds;
        public NamedEntityInText originalEntity;
        public boolean isTaboo = false;
        public double totalDocs;

        private MentionOccurrence(List<AnchorDocument> anchorDocs,
                NamedEntityInText originalEntity) {
            this.anchorDocs = anchorDocs;
            this.docIds = new int[anchorDocs.size()];
            this.originalEntity = originalEntity;
            collectIds();
            this.totalDocs = anchorDocs.size();
        }

        private void collectIds() {
            for(int i=0; i<anchorDocs.size(); i++){
                docIds[i] = anchorDocs.get(i).id;
            }
        }

        public String toString() {
            return originalEntity.getLabel();
        }
    }

    public CandidateSearcher getCandidateSearcher() {
        return searcher;
    }

}
