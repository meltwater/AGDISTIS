package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class AgdistisDisambiguator {

    private final Logger LOGGER = LoggerFactory
            .getLogger(AgdistisDisambiguator.class);

    private static String resourceURI = "http://dbpedia.org/resource/";
    private static final String projectName = "fhai";
    CandidateSearcher cs;

    public static final double ANCHOR_THRES = 0.10;

    // all of the following should add up to 1.0;
    public static final double LINK_SHARE = 0.50;
    public static final double PR_SHARE = 0.25;
    public static final double ANCHOR_SHARE = 0.25;

    public AgdistisDisambiguator() {
        try {
            cs = new CandidateSearcher();
        } catch (IOException e) {
            LOGGER.error("problem initializing the searcher, check in the index.");
        }
    }

    public void run(final Document document) {
        Relatedness relatednessP = new Relatedness();
        // search for candidate AnchorDocument given a NE mention
        Collection<AnchorOccurrence> anchorOccurences = collectAnchorOccurences(document
                .getNamedEntitiesInText());
        List<AnchorDocument> trustedDocs = collectTrustedDocs(anchorOccurences);
        double[] trustedWeights = calculateLinkWeight(trustedDocs, relatednessP);

        for (AnchorOccurrence anchorOccurence : anchorOccurences) {

            double senseScoreSum = 0;
            for (final AnchorDocument sense : anchorOccurence.anchorDocs) {
                // spec handling of lists
                if(trustedDocs.size() == 0)continue;
                sense.setLinkShare(senseScore(trustedDocs, trustedWeights,
                        sense, relatednessP, anchorOccurence));
                senseScoreSum += sense.getLinkShare();
            }

            // normalize and calculate final score
            for (final AnchorDocument sense : anchorOccurence.anchorDocs) {
//                if(senseScoreSum > 0){
//                    // save from divide by zero
//                    sense.setLinkShare(sense.getLinkShare() / senseScoreSum);
//                }
                calculateCombinedScore(sense);
            }
        }

        chooseTheTop(anchorOccurences);
    }

    private void calculateCombinedScore(AnchorDocument sense) {
        double score = (LINK_SHARE * sense.getLinkShare())
                + (PR_SHARE * sense.getPageRank())
                + (ANCHOR_SHARE * sense.getAnchorProb());
        sense.setScore(score);
    }

    private double senseScore(List<AnchorDocument> allCandidates,
            double[] trustedWeights, AnchorDocument sense,
            Relatedness relatednessP, AnchorOccurrence anchorOccurence) {

        double sum = 0.0;
        double sumW = 0.0;
        for (int i = 0; i < allCandidates.size(); i++) {
            final AnchorDocument trustedCand = allCandidates.get(i);
            if (trustedCand.id == sense.id) {
                continue;
            }
            final double r = relatednessP.getRelatedness(sense, trustedCand);
            final double w = trustedWeights[i];
            sum += w * r * anchorOccurence.retrivalProb;
            sumW += w ;
        }

        if(sumW == 0)return 0; // save from divide by 0
        return (sum / sumW);
    }

    private double[] calculateLinkWeight(List<AnchorDocument> trustedDocs,
            Relatedness relatednessP) {

        final int trustedCount = trustedDocs.size();
        final double[] trustWeights = new double[trustedCount];

        for (int i = 0; i < trustedCount; i++) {
            double sum = 0.0;
            for (int j = 0; j < trustedCount; j++) {
                if (i == j) {
                    continue;
                }

                final double r = relatednessP.getRelatedness(
                        trustedDocs.get(i), trustedDocs.get(j));
                sum += r;
                
            }
            
            trustWeights[i] = sum / trustedCount;
        }

        return trustWeights;

    }

    private List<AnchorDocument> collectTrustedDocs(
            Collection<AnchorOccurrence> anchorOccurences) {
        List<AnchorDocument> trustedDocs = Lists.newArrayList();
        for (AnchorOccurrence anchorOccurence : anchorOccurences) {

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

    private void chooseTheTop(Collection<AnchorOccurrence> anchorOccurences) {

        for (AnchorOccurrence anchorOccurrence : anchorOccurences) {

            final AnchorDocument topScored = getTopCandidate(anchorOccurrence.anchorDocs);
            if (null == topScored) {
                continue;
            }
            final NamedEntityInText originalEntity = anchorOccurrence.originalEntity;
            final String candidateURI = topScored.subject;
            String canonicalName = extractLabel(topScored.subject);
            if (canonicalName == null) {
                canonicalName = "";
            }
            originalEntity.setCanonicalName(canonicalName);
            originalEntity.setNamedEntity(candidateURI);
            originalEntity.setAuthorityWeight(topScored.getScore());
            originalEntity.setDisambiguatedTypes(Lists.newArrayList());
//            System.out.println("original: " + originalEntity.getLabel()
//                    + " assinged " + candidateURI + " anchor score "
//                    + topScored.getAnchorProb() + " sense: "
//                    + topScored.getLinkShare() + " pagrank "
//                    + topScored.getPageRank() + " score "
//                    + topScored.getScore());
        }

    }

    private String extractLabel(String subject) {

        int resource = subject.indexOf(resourceURI);
        if (resource == -1) {
            resource = subject.lastIndexOf('/') + 1;
        } else {
            resource = resource + resourceURI.length();
        }

        String label = subject.substring(resource);
        if (label.startsWith(projectName)) {
            label = label.substring(projectName.length() + 1);
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
            if (anchorDoc.getScore() > topProb) {
                topProb = anchorDoc.getScore();
                topDocument = anchorDoc;
            }
        }
        if (null == topDocument && anchorDocs.size() > 0) {
            topDocument = anchorDocs.get(0);
        }
        return topDocument;
    }

    private Collection<AnchorOccurrence> collectAnchorOccurences(
            NamedEntitiesInText namedEntitiesInText) {
        List<AnchorOccurrence> anchorOccurences = new ArrayList<AgdistisDisambiguator.AnchorOccurrence>();
        double retrievalProbSum = 0;
        for (NamedEntityInText ne : namedEntitiesInText) {
            
            List<AnchorDocument> anchorDocuments = candidateSearch(ne
                    .getSurfaceForm());
            retrievalProbSum += anchorDocuments.size();
            anchorOccurences.add(new AnchorOccurrence(anchorDocuments, ne));
            /**
             * Collect in links ids
             */
            for (AnchorDocument anchorDocument : anchorDocuments) {
                anchorDocument.inLinks = searchForInLinks(anchorDocument);
            }

            /**
             * Normalize PageRank
             */
            if (anchorDocuments.size() <= 1)
                continue; // Normalization not requires

            double pageRankSum = 0d;
            for (AnchorDocument anchorDocument : anchorDocuments) {
                pageRankSum += anchorDocument.getPageRank();
            }
            for (AnchorDocument anchorDocument : anchorDocuments) {
                anchorDocument.setPageRank(anchorDocument.getPageRank()
                        / pageRankSum);
            }

        }
        for(AnchorOccurrence anchorOccurrence:anchorOccurences){
            anchorOccurrence.retrivalProb = (double)anchorOccurrence.anchorDocs.size() / retrievalProbSum;
        }
        
        return anchorOccurences;
    }

    private List<Integer> searchForInLinks(AnchorDocument anchorDocument) {
        List<Integer> inLinkIds = Lists.newArrayList();
        List<AnchorDocument> inlinkDocs = cs
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
        List<AnchorDocument> results = cs.searchAnchorText(anchorAsObject);
        if (null == results || results.isEmpty()) {
            // lowercased anchor
            anchorAsObject = anchor.toLowerCase();
            results = cs.searchAnchorText(anchorAsObject);
        }
        if (null == results) {
            results = new ArrayList<AnchorDocument>();
        }
        return results;
    }

    public CandidateSearcher getCandidateSearcher() {
        return cs;
    }

    private class AnchorOccurrence {
        List<AnchorDocument> anchorDocs;
        public NamedEntityInText originalEntity;
        
        public double retrivalProb;

        private AnchorOccurrence(List<AnchorDocument> anchorDocs,
                NamedEntityInText originalEntity) {
            this.anchorDocs = anchorDocs;
            this.originalEntity = originalEntity;
        }

        public String toString() {
            return originalEntity.getLabel();
        }
    }

}
