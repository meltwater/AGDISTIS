package org.aksw.agdistis.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.aksw.agdistis.datatypes.AnchorDocument;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

public class CandidateSearcher {


    private final Logger log = LoggerFactory.getLogger(TripleIndex.class);

    public static final String FIELD_NAME_ID = "id";
    public static final String FIELD_NAME_IDTYPE = "id_type";
    
    public static final String FIELD_NAME_ANCHOR_PROB = "anchor_prob";
    public static final String FIELD_NAME_PAGE_RANK = "page_rank";

    public static final String FIELD_NAME_SUBJECT = "subject";
    public static final String FIELD_NAME_PREDICATE = "predicate";
    public static final String FIELD_NAME_OBJECT_URI = "object_uri";
    public static final String FIELD_NAME_OBJECT_LITERAL = "object_literal";

    public static final String warmUpQueryPredicate = "http://www.w3.org/2000/01/rdf-schema#label";

    private static final Set<String> _LUCENE_KEYWORDS = Sets.newHashSet("AND",
            "OR", "NOT", "TO");
    public static final String FIELD_FREQ = "freq";

    

    private final int anchorTextLimit = 500;
    private final int inLinksLimit = 500;
    
    public double ANCHOR_MATCH_THRES = 0.90;

    private final Directory directory;
    private final IndexSearcher isearcher;
    private final DirectoryReader ireader;
    private final UrlValidator urlValidator;
    private final Cache<BooleanQuery, List<AnchorDocument>> cache;
    StringUtils isInt = new StringUtils();

    public CandidateSearcher() throws IOException {
        final String index = AGDISTISConfiguration.INSTANCE.getMainIndexPath()
                .toString();
        log.info("The index will be loaded from: " + index);
        directory = new MMapDirectory(new File(index));
        final long start = System.currentTimeMillis();
        ireader = DirectoryReader.open(directory);
        log.info("Index loaded in {} msec", System.currentTimeMillis() - start);
        isearcher = new IndexSearcher(ireader);
        urlValidator = new UrlValidator();

        cache = CacheBuilder
                .newBuilder()
                .maximumSize(
                        AGDISTISConfiguration.INSTANCE
                                .getTripleIndexCacheSize())
                .expireAfterWrite(30, TimeUnit.MINUTES).build();
    }
    
    public List<AnchorDocument> search(final String id, final int maxNumberOfResults){
        final BooleanQuery bq = new BooleanQuery();
        List<AnchorDocument> results = new ArrayList<AnchorDocument>();
        final Query tq = new TermQuery(new Term(FIELD_NAME_ID,id));
        bq.add(tq, BooleanClause.Occur.MUST);
        try {
            results = getFromIndex(maxNumberOfResults, bq);
         // use the cache
            if (null == (results = cache.getIfPresent(bq))) {
                results = getFromIndex(maxNumberOfResults, bq);
                cache.put(bq, results);
            }
        } catch (final IOException ioe) {
            log.error(
                    "I/O exception occurred while reading from the index. Corrupt?. StackTrace {}",
                    ExceptionUtils.getStackTrace(ioe), id);
            return Lists.newLinkedList();
        } 
        return results;
        
    }
    
    
    public List<AnchorDocument> searchInLinks(final String subject){
        final BooleanQuery bq = new BooleanQuery();
        List<AnchorDocument> results = new ArrayList<AnchorDocument>();
        final Query subjectQuery = new TermQuery(new Term(FIELD_NAME_OBJECT_URI,subject));
        bq.add(subjectQuery, BooleanClause.Occur.MUST);
        
        final Query predicateQuery = new TermQuery(new Term(FIELD_NAME_PREDICATE,
                IndexCreator.inlinkURI));
        bq.add(predicateQuery, BooleanClause.Occur.MUST);
        
        try {
            results = getFromIndex(inLinksLimit, bq);
         // use the cache
            if (null == (results = cache.getIfPresent(bq))) {
                results = getFromIndex(inLinksLimit, bq);
                cache.put(bq, results);
            }
        } catch (final IOException ioe) {
            log.error(
                    "I/O exception occurred while reading from the index. Corrupt?. StackTrace {}",
                    ExceptionUtils.getStackTrace(ioe), subject);
            return Lists.newLinkedList();
        } 
        return results;
    }
    
    public List<AnchorDocument> searchAnchorText(final String anchorText){
        List<AnchorDocument> results = search(null, IndexCreator.anchorTextURI, anchorText, anchorTextLimit);
        if (results.size() == 0) {
            return results;
        }
        Map<Long, List<AnchorDocument>> targetAnchorTextToPos = new HashMap<Long, List<AnchorDocument>>();
        // double[] = {matchProb, startPos, endPos}
        double maxScore = 0;
        Long topTargetAnchorHash = null;
        for (AnchorDocument doc : results) {
            Long hash = Hasher.hash(doc.getAnchorText());
            List<AnchorDocument> metrics = targetAnchorTextToPos.get(hash);
            if (null == metrics) {
                double matchScore = calculateMatch(anchorText,
                        doc.getAnchorText());
                if(matchScore == 0){
                    matchScore = calculateMatch(doc.getAnchorText(), anchorText);
                }
                if (matchScore > maxScore) {
                    maxScore = matchScore;
                    topTargetAnchorHash = hash;
                    metrics = new ArrayList<AnchorDocument>();
                    metrics.add(doc);
                    targetAnchorTextToPos.put(hash, metrics);
                }
            } else {
                metrics.add(doc);
            }
        }
        if (maxScore >= ANCHOR_MATCH_THRES) {
            return targetAnchorTextToPos.get(topTargetAnchorHash);
        }
        return null; // This should be very rare;

    }
    
    private double calculateMatch(String anchorText, String targetAnchorText) {
        
        int index = targetAnchorText.indexOf(anchorText);
        if(index == -1)return 0d;
        // there is match and targetAnchorText is longer than the anchorText
        double match = 0;
        for(int i=0; i<anchorText.length(); i++){
            if(anchorText.charAt(i) == targetAnchorText.charAt( i + index)){
                match++;
            }
        }
        double anchorMatchProb = Math.max(0, match/(double)anchorText.length());
        
        double targetMatchProb = targetAnchorText.length();
        targetMatchProb = (targetMatchProb - match)/targetMatchProb;
        targetMatchProb = Math.max(0, 1.0 - targetMatchProb);
        
        return targetMatchProb * anchorMatchProb;
    }


    public List<AnchorDocument> search(final String subject,
            final String predicate, String object, final int maxNumberOfResults) {
        final BooleanQuery bq = new BooleanQuery();
        List<AnchorDocument> triples;
        try {
            if (subject != null) {
                final Query q = new TermQuery(new Term(FIELD_NAME_SUBJECT,
                        subject));
                bq.add(q, BooleanClause.Occur.MUST);
            }
            if (predicate != null) {
                final Query q = new TermQuery(new Term(FIELD_NAME_PREDICATE,
                        predicate));
                bq.add(q, BooleanClause.Occur.MUST);
            }

            if ((object != null) && !StringUtils.isBlank(object)) {
                Query q = null;
                if(urlValidator.isValid(object)){
                    q  = new TermQuery(new Term(CandidateSearcher.FIELD_NAME_OBJECT_URI, object));
                }else{
                    final QueryParser parser = new QueryParser(IndexCreator.LUCENE_VERSION, CandidateSearcher.FIELD_NAME_OBJECT_LITERAL, new LiteralAnalyzer(IndexCreator.LUCENE_VERSION));
                    parser.setDefaultOperator(QueryParser.Operator.AND);
                    q = parser.parse(QueryParserBase.escape(object = escapeLuceneKeywords(object)));
                }
                bq.add(q, BooleanClause.Occur.MUST);
            }
            // use the cache
            if (null == (triples = cache.getIfPresent(bq))) {
                triples = getFromIndex(maxNumberOfResults, bq);
                cache.put(bq, triples);
            }
            return triples;
        } catch (final IOException ioe) {
            log.error(
                    "I/O exception occurred while reading from the index. Corrupt?. StackTrace {}",
                    ExceptionUtils.getStackTrace(ioe), subject);
            return Lists.newLinkedList();
        } catch (final ParseException pe) {
            log.error("Unable to parse the object from the triple <{},{},{}>.",
                    subject, predicate, object);
            return Lists.newLinkedList();
        } catch (final Exception pe) {
            log.error("Unable to process the query triple <{},{},{}>.",
                    subject, predicate, object);
            return Lists.newLinkedList();
        }

    }

    private List<AnchorDocument> getFromIndex(final int maxNumberOfResults,
            final BooleanQuery bq) throws IOException  {
        
        final TopScoreDocCollector collector = TopScoreDocCollector.create(
                maxNumberOfResults, true);
        final List<AnchorDocument> triples = new LinkedList<AnchorDocument>();
        
        try {
            log.trace("start asking index...");
            isearcher.search(bq, collector);
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;

            
            String idStr, s, p, o, probStr, pageRankStr;
            for (final ScoreDoc hit : hits) {
                final Document hitDoc = isearcher.doc(hit.doc);
                idStr = hitDoc.get(FIELD_NAME_ID);
                s = hitDoc.get(FIELD_NAME_SUBJECT);
                p = hitDoc.get(FIELD_NAME_PREDICATE);
                o = hitDoc.get(FIELD_NAME_OBJECT_URI);
                if (o == null) {
                    o = hitDoc.get(FIELD_NAME_OBJECT_LITERAL);
                }
                
                int id = -1;
                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    id = -1;
                }
                double anchorProb = 0d;
                probStr = hitDoc.get(FIELD_NAME_ANCHOR_PROB);
                if(null != probStr){
                    try {
                        anchorProb = Double.parseDouble(probStr);
                    } catch (NumberFormatException e) {
                        anchorProb = 0d;
                    }
                }
                double pageRank = 0d;
                pageRankStr = hitDoc.get(FIELD_NAME_PAGE_RANK);
                if(null != pageRankStr){
                    try {
                        pageRank = Double.parseDouble(pageRankStr);
                    } catch (NumberFormatException e) {
                        pageRank = 0d;
                    }
                }
                
                final AnchorDocument triple = new AnchorDocument(id, s, p, o, anchorProb, pageRank);
                triples.add(triple);
            }
            log.trace("finished asking index...");
        } catch (IOException e) {
            log.error("problem searching the lucene index for a boolean query "+bq);
            throw new IOException(e.getLocalizedMessage());
        }
        return triples;
    }

    public void close() throws IOException {
        ireader.close();
        directory.close();
    }

    public DirectoryReader getIreader() {
        return ireader;
    }

    private String escapeLuceneKeywords(String queryString) {
        final String[] tokens = StringUtils.split(queryString);
        return Stream.of(tokens).parallel()
                .map(token -> escapeLuceneKeyword(token))
                .collect(Collectors.joining(" "));
    }

    private String escapeLuceneKeyword(String token) {
        if (_LUCENE_KEYWORDS.contains(token)) {
            return "\\" + token;
        }
        return token;
    }

}
