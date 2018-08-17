package org.aksw.agdistis.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aksw.agdistis.datatypes.AnchorDocument;
import org.aksw.agdistis.datatypes.DisambiguatedDocument;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.CandidateSearcher;



public class AgdistisDisambiguator {
    
    
    CandidateSearcher cs;
    
    
    public void run(final Document document){
        // search for candidate AnchorDocument given a NE mention
        Collection<AnchorOccurrence> anchorOccurences = collectAnchorOccurences(document.getNamedEntitiesInText());
        disambiguate(anchorOccurences);
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
        
        String anchorAsObject = "\""+anchor+"\"@en";
        List<AnchorDocument> results = cs.search(null, null, anchorAsObject);
        if(null == results || results.isEmpty()){
            // lowercased anchor
            anchorAsObject = "\""+anchor.toLowerCase()+"\"@en";
            results = cs.search(null, null, anchorAsObject);
        }
        return results;
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
