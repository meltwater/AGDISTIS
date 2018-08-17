package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.agdistis.datatypes.AnchorDocument;
import org.aksw.agdistis.datatypes.DisambiguatedDocument;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.CandidateSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;



public class AgdistisDisambiguator {
    
    private final Logger LOGGER = LoggerFactory.getLogger(AgdistisDisambiguator.class);
    
    private static String resourceURI = "http://dbpedia.org/resource/";
    private static final String projectName = "fhai";
    CandidateSearcher cs;
    
    public AgdistisDisambiguator() {
        try {
            cs = new CandidateSearcher();
        } catch (IOException e) {
            LOGGER.error("problem initializing the searcher, check in the index.");
        }
    }
    
    
    public void run(final Document document){
        // search for candidate AnchorDocument given a NE mention
        Collection<AnchorOccurrence> anchorOccurences = collectAnchorOccurences(document.getNamedEntitiesInText());
        disambiguateBasedOnAnchor(anchorOccurences);
    }


    private void disambiguateBasedOnAnchor(Collection<AnchorOccurrence> anchorOccurences) {
        
        for(AnchorOccurrence anchorOccurrence: anchorOccurences){
            final AnchorDocument topScored = getTopSenseProbabilityCandidate(anchorOccurrence.anchorDocs);
            if(null == topScored){
                continue;
            }
            final NamedEntityInText originalEntity = anchorOccurrence.originalEntity;
            final String candidateURI = topScored.subject;
            String canonicalName = extractLabel(topScored.subject);
            if(canonicalName == null){
                canonicalName = "";
            }
            originalEntity.setCanonicalName(canonicalName);
            originalEntity.setNamedEntity(candidateURI);
            originalEntity.setAuthorityWeight(topScored.anchorProb);
            originalEntity.setDisambiguatedTypes(Lists.newArrayList());
        }
        
    }


    private String extractLabel(String subject) {
        
        int resource = subject.indexOf(resourceURI);
        if(resource == -1){
            resource = subject.lastIndexOf('/')  + 1;
        }else{
            resource = resource + resourceURI.length();
        }
       
        String label = subject.substring(resource);
        if(label.startsWith(projectName)){
            label = label.substring(projectName.length() + 1);
        }
        return normaliseTitle(label);
    }
    
    public static String normaliseTitle(String title) {
        if (title.length() == 0) return null;
        if (title.contains("\n")) return null; // some parsing error
        
        StringBuffer s = new StringBuffer() ;
        s.append(Character.toUpperCase(title.charAt(0))) ;
        s.append(title.substring(1).replace('_', ' ')) ;

        return s.toString() ;
    }


    private AnchorDocument getTopSenseProbabilityCandidate(
            List<AnchorDocument> anchorDocs) {
        AnchorDocument topDocument = null;
        double topProb = 0;
        for(AnchorDocument anchorDoc:anchorDocs){
            if(anchorDoc.anchorProb > topProb){
                topProb = anchorDoc.anchorProb;
                topDocument = anchorDoc;
            }
        }
        if(null == topDocument && anchorDocs.size() > 0){
            topDocument  = anchorDocs.get(0);
        }
        return topDocument;
    }


    private void disambiguate(Collection<AnchorOccurrence> anchorOccurences) {
        
        List<DisambiguatedDocument[]> candidatesList = collectCandidateSenses(anchorOccurences);
        
        final Map<String, DisambiguatedDocument> bestSenseByStaticScore = new HashMap<>();
        for (final DisambiguatedDocument[] candidates : candidatesList) {
          bestSenseByStaticScore.put(candidates[0].getAnchor(), candidates[0]);
        }
    }
    
    
    private List<DisambiguatedDocument[]> collectCandidateSenses(Collection<AnchorOccurrence> anchorOccurrences) {
          final List<DisambiguatedDocument[]> candidatesList = new ArrayList<>();
          for (final AnchorOccurrence ao : anchorOccurrences) {
            final List<AnchorDocument> stats = ao.anchorDocs;
            final int count = stats.size(); // all senses considered
            // shouldn't happen, but:
            // TODO: This does happen!
            if (count == 0) {
              continue;
            }
            final DisambiguatedDocument[] candidates = new DisambiguatedDocument[count];
            candidatesList.add(candidates);
            final String anchor = ao.originalForm;
            for (int i = 0; i < count; i++) {
              candidates[i] = new DisambiguatedDocument(anchor, ao.indexes.size(), stats.get(i).id, stats.get(i).subject, stats.get(i).anchorProb,stats.get(i).anchorProb);
            }
          }
          return candidatesList;
        }
    
    
    public double getRelatedness(int i, int j){
//        TODO complete relatedness calculation
        List<AnchorDocument> iDocs = cs.search(String.valueOf(i), 100);
        List<AnchorDocument> jDocs = cs.search(String.valueOf(j), 100);
        
        return 0;
    }


    private Collection<AnchorOccurrence> collectAnchorOccurences(
            NamedEntitiesInText namedEntitiesInText) {
        List<AnchorOccurrence> anchorOccurences = new ArrayList<AgdistisDisambiguator.AnchorOccurrence>();
        for(NamedEntityInText ne:namedEntitiesInText){
            final List<AnchorDocument> anchorDocuments = candidateSearch(ne.getSurfaceForm());
            anchorOccurences.add(new AnchorOccurrence(anchorDocuments, ne));
        }
        return anchorOccurences;
    }


    private List<AnchorDocument> candidateSearch(String anchor) {
        
//        String anchorAsObject = "\""+anchor+"\"@en";
        String anchorAsObject = anchor;
        List<AnchorDocument> results = cs.search(null, null, anchorAsObject);
        if(null == results || results.isEmpty()){
            // lowercased anchor
            anchorAsObject = "\""+anchor.toLowerCase()+"\"@en";
            results = cs.search(null, null, anchorAsObject);
        }
        if(null == results){
            results = new ArrayList<AnchorDocument>();
        }
        return results;
    }
    
    public CandidateSearcher getCandidateSearcher(){
        return cs;
    }
    
    
    private class AnchorOccurrence {
        List<AnchorDocument> anchorDocs;
        public String originalForm;
        public NamedEntityInText originalEntity;
        private final List<Integer> indexes = new ArrayList<>();

        private AnchorOccurrence(List<AnchorDocument> anchorDocs, NamedEntityInText originalEntity) {
          this.anchorDocs = anchorDocs;
          this.originalEntity = originalEntity;
          this.originalForm = originalEntity.getSurfaceForm();
        }
    }

}
