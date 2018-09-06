package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.aksw.agdistis.datatypes.AnchorDocument;
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

    private double logW = 3332693;//WikiSize
    
    
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
        List<AnchorDocument> trustedDocs = collectTrustedDocs(anchorOccurences);
        double[] trustedWeights = calculateLinkWeight(trustedDocs);
        
        for(AnchorOccurrence anchorOccurence: anchorOccurences){
            if (anchorOccurence.anchorDocs.size() == 1) {
                continue;
              }
              for (final AnchorDocument sense : anchorOccurence.anchorDocs) {
                // spec handling of lists
                senseScore(trustedDocs, trustedWeights, sense);
              }
        }
        
        chooseTheTop(anchorOccurences);
    }


    

    private void senseScore(List<AnchorDocument> allCandidates,
            double[] trustedWeights, AnchorDocument sense) {

        double sum = 0.0;
        double sumW = 0.0;
        for (int i = 0; i < allCandidates.size(); i++) {
          final AnchorDocument trustedCand = allCandidates.get(i);
          if (trustedCand.id == sense.id) {
            continue;
          }
          final double r = getRelatedness(sense.inLinks, trustedCand.inLinks);
          final double w = trustedWeights[i];
          sum += w * r;
          sumW += w;
        }
        if (sumW > 0.0) {
          sense.setSenseScoreContext(sum / sumW);
        }
      
    }


    private double[] calculateLinkWeight(List<AnchorDocument> trustedDocs) {
        
        
        final int trustedCount = trustedDocs.size();
        final double[] trustWeights = new double[trustedCount];
        for (int i = 0; i < trustedCount; i++) {
          double sum = 0.0;
          for (int j = 0; j < trustedCount; j++) {
            if (i == j) {
              continue;
            }
            final List<Integer> idI = trustedDocs.get(i).inLinks;
            final List<Integer> idJ = trustedDocs.get(j).inLinks;
            
            final double r = getRelatedness(idI, idJ);
            System.out.println(" dist "+r+" with: "+trustedDocs.get(j).subject);
            sum += r;
          }
          
          trustWeights[i] = sum / trustedCount;
          trustWeights[i] *= trustedDocs.get(i).anchorProb;
        }
        double avgTrustWeight = 0.0;
        for (final double d : trustWeights) {
          avgTrustWeight += d;
        }
        avgTrustWeight /= trustWeights.length;
        // Set them to 1 to avoid divisionByZero later
        if (avgTrustWeight == 0) {
          for (int i = 0; i < trustedCount; i++) {
            trustWeights[i] = 1;
          }
        }
        return trustWeights;
        
        
        
    }


    private List<AnchorDocument> collectTrustedDocs(
            Collection<AnchorOccurrence> anchorOccurences) {
        List<AnchorDocument> trustedDocs = Lists.newArrayList();
        for(AnchorOccurrence anchorOccurence:anchorOccurences){
            
            /**
             * Filter by high sense prob 
             */
            List<AnchorDocument> anchorDocuments = anchorOccurence.anchorDocs.stream()
            .filter(senseCandidate -> (senseCandidate.anchorProb > 0.10)).collect(Collectors.toList());
            trustedDocs.addAll(anchorDocuments);
        }
        return trustedDocs;
    }


    private void chooseTheTop(Collection<AnchorOccurrence> anchorOccurences) {
        
        for(AnchorOccurrence anchorOccurrence: anchorOccurences){
            
            final AnchorDocument topScored = getTopSenseContextProbabilityCandidate(anchorOccurrence.anchorDocs);
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
            originalEntity.setAuthorityWeight(topScored.getSenseScoreContext());
            originalEntity.setDisambiguatedTypes(Lists.newArrayList());
            System.out.println("original: "+originalEntity.getLabel() + " assinged "+candidateURI+ " anchor score "+topScored.anchorProb+" sense: "+topScored.getSenseScoreContext()+" pagrank "+topScored.pageRank);
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


    private AnchorDocument getTopSenseContextProbabilityCandidate(
            List<AnchorDocument> anchorDocs) {
        AnchorDocument topDocument = null;
        double topProb = 0;
        for(AnchorDocument anchorDoc:anchorDocs){
            if(anchorDoc.getSenseScoreContext() > topProb){
                topProb = anchorDoc.getSenseScoreContext();
                topDocument = anchorDoc;
            }
        }
        if(null == topDocument && anchorDocs.size() > 0){
            topDocument  = anchorDocs.get(0);
        }
        return topDocument;
    }


    public double getRelatedness(List<Integer> A, List<Integer> B){
        if(A == null || B == null)return 0d;
        final double distance = getDistance(500, A.size(), B.size(), A, B);
        return distance;
    }
    
    private double getDistance(int maxLinkToConsider, int countA, int countB, List<Integer> A, List<Integer> B) {
        int i = 0;
        int j = 0;
        int countCommon = 0;
        if ((maxLinkToConsider == -1) || ((countA <= maxLinkToConsider) && (countB <= maxLinkToConsider))) {
          /**
           * search in full id ranges
           */
          while ((i < countA) && (j < countB)) {
            final int diff = A.get(i) - B.get(j);
            if (diff < 0) {
              i++;
            } else if (diff > 0) {
              j++;
            } else {
              countCommon++;
              i++;
              j++;
            }
          }
          return distance(countA, countB, countCommon);
        } else {
          /**
           * subsampled matching and extrapolating result
           */
          int countA2 = countA;
          int countB2 = countB;
          double ratioA = 1;
          if (countA > maxLinkToConsider) {
            ratioA = (double) countA / maxLinkToConsider;
            countA2 = maxLinkToConsider;
          }
          double ratioB = 1;
          if (countB > maxLinkToConsider) {
            ratioB = (double) countB / maxLinkToConsider;
            countB2 = maxLinkToConsider;
          }
          double iD = i;
          double jD = j;
          while ((i < countA) && (j < countB)) {
            final int diff = A.get(i) - B.get(j);
            if (diff < 0) {
              iD += ratioA;
              i = (int) iD;
            } else if (diff > 0) {
              jD += ratioB;
              j = (int) jD;
            } else {
              countCommon++;
              iD += ratioA;
              i = (int) iD;
              jD += ratioB;
              j = (int) jD;
            }
          }
          return distance(countA2, countB2, countCommon);
        }
      }
    
    
    private double distance(int countA, int countB, int countCommon) {
        if (countCommon == 0) {
          return 0;
        }

        final int maxCount = Math.max(countA, countB);
        final int minCount = Math.min(countA, countB);

        // normalized google distance (logW: log(kgsize))
        return (Math.log(maxCount) - Math.log(countCommon)) / (logW - Math.log(minCount));
      }


    private Collection<AnchorOccurrence> collectAnchorOccurences(
            NamedEntitiesInText namedEntitiesInText) {
        List<AnchorOccurrence> anchorOccurences = new ArrayList<AgdistisDisambiguator.AnchorOccurrence>();
        for(NamedEntityInText ne:namedEntitiesInText){
            List<AnchorDocument> anchorDocuments = candidateSearch(ne.getSurfaceForm());
            
            /**
             * Collect in links ids
             */
            for(AnchorDocument anchorDocument:anchorDocuments){
                anchorDocument.inLinks = searchForInLinks(anchorDocument);
            }
            anchorOccurences.add(new AnchorOccurrence(anchorDocuments, ne));
        }
        return anchorOccurences;
    }


    private List<Integer> searchForInLinks(AnchorDocument anchorDocument) {
        List<Integer> inLinkIds = Lists.newArrayList();
        List<AnchorDocument> inlinkDocs = cs.searchInLinks(anchorDocument.subject);
        if(null == inlinkDocs)return inLinkIds;
        for(AnchorDocument inLinkDoc:inlinkDocs){
            inLinkIds.add(inLinkDoc.id);
        }
        return inLinkIds;
    }


    private List<AnchorDocument> candidateSearch(String anchor) {
        
//        String anchorAsObject = "\""+anchor+"\"@en";
        String anchorAsObject = anchor;
        List<AnchorDocument> results = cs.searchAnchorText(anchorAsObject);
        if(null == results || results.isEmpty()){
            // lowercased anchor
            anchorAsObject = anchor.toLowerCase();
            results = cs.searchAnchorText(anchorAsObject);
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
        public NamedEntityInText originalEntity;

        private AnchorOccurrence(List<AnchorDocument> anchorDocs, NamedEntityInText originalEntity) {
          this.anchorDocs = anchorDocs;
          this.originalEntity = originalEntity;
        }
        
        public String toString(){
            return originalEntity.getLabel();
        }
    }

}
