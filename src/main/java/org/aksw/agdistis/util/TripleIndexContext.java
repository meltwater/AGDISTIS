package org.aksw.agdistis.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class TripleIndexContext {

  private static final Version LUCENE44 = Version.LUCENE_44;

  private final org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndexContext.class);

  public static final String FIELD_NAME_CONTEXT = "USE_CONTEXT";
  public static final String FIELD_NAME_SURFACE_FORM = "SURFACE_FORM";
  public static final String FIELD_NAME_URI = "URI";
  public static final String FIELD_NAME_URI_COUNT = "URI_COUNT";

  private final int defaultMaxNumberOfDocsRetrievedFromIndex = 100;

  private final Directory directory;
  private final IndexSearcher isearcher;
  private final DirectoryReader ireader;
  private final Cache<BooleanQuery, List<Triple>> cache;

  public TripleIndexContext() throws IOException {
    final String index = AGDISTISConfiguration.INSTANCE.getIndexByContextPath().toString();
    log.info("The index will be here: " + index);

    directory = new MMapDirectory(new File(index));
    ireader = DirectoryReader.open(directory);
    isearcher = new IndexSearcher(ireader);
    new UrlValidator();

    cache = CacheBuilder.newBuilder().maximumSize(50000).build();
  }

  public List<Triple> search(final String subject, final String predicate, final String object) {
    return search(subject, predicate, object, defaultMaxNumberOfDocsRetrievedFromIndex);
  }

  public List<Triple> search(final String subject, final String predicate, final String object,
      final int maxNumberOfResults) {
    final BooleanQuery bq = new BooleanQuery();
    List<Triple> triples = new ArrayList<Triple>();
    try {
      if ((subject != null) && subject.equals("http://aksw.org/notInWiki")) {
        log.error(
            "A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
      }
      if (subject != null) {
        // TermQuery tq = new TermQuery(new Term(FIELD_NAME_CONTEXT,
        // subject));
        // bq.add(tq, BooleanClause.Occur.MUST);
        Query q = null;
        // TermQuery tq = new TermQuery(new
        // Term(FIELD_NAME_SURFACE_FORM, predicate));
        final Analyzer analyzer = new LiteralAnalyzer(LUCENE44);
        final QueryParser parser = new QueryParser(LUCENE44, FIELD_NAME_CONTEXT, analyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND);
        q = parser.parse(QueryParserBase.escape(subject));
        bq.add(q, BooleanClause.Occur.MUST);
      }
      if (predicate != null) {

        // Query q = null;
        final TermQuery tq = new TermQuery(new Term(FIELD_NAME_SURFACE_FORM, predicate));
        /*
         *
         * Analyzer analyzer = new LiteralAnalyzer(LUCENE44); //KeywordAnalyzer analyzer = new KeywordAnalyzer();
         * QueryParser parser = new QueryParser(LUCENE44, FIELD_NAME_URI, analyzer);
         * parser.setDefaultOperator(QueryParser.Operator.OR); q = parser.parse(QueryParserBase.escape(predicate));
         * bq.add(q, BooleanClause.Occur.MUST);
         */
        // TermQuery tq = new TermQuery(new Term(FIELD_NAME_URI,
        // predicate));
        bq.add(tq, BooleanClause.Occur.SHOULD);
      }
      if (object != null) {
        final TermQuery tq = new TermQuery(new Term(FIELD_NAME_URI_COUNT, object));
        bq.add(tq, BooleanClause.Occur.MUST);
      }
      // use the cache
      if (null == (triples = cache.getIfPresent(bq))) {
        triples = getFromIndex(maxNumberOfResults, bq);
        if (triples == null) {
          return new ArrayList<Triple>();
        }
        cache.put(bq, triples);
      }

    } catch (final Exception e) {
      log.error("{} -> {}", e.getLocalizedMessage(), subject);

    }
    return triples;
  }

  private List<Triple> getFromIndex(final int maxNumberOfResults, final BooleanQuery bq) throws IOException {
    // log.debug("\t start asking index...");
    // Similarity BM25Similarity = new BM25Similarity();
    // isearcher.setSimilarity(BM25Similarity);
    final ScoreDoc[] hits = isearcher.search(bq, null, maxNumberOfResults).scoreDocs;

    if (hits.length == 0) {
      return new ArrayList<Triple>();
    }
    final List<Triple> triples = new ArrayList<Triple>();
    String s, p, o;
    for (final ScoreDoc hit : hits) {
      final Document hitDoc = isearcher.doc(hit.doc);
      s = hitDoc.get(FIELD_NAME_CONTEXT);
      p = hitDoc.get(FIELD_NAME_URI);
      o = hitDoc.get(FIELD_NAME_URI_COUNT);
      final Triple triple = new Triple(s, p, o);
      triples.add(triple);
    }
    log.debug("\t finished asking index...");

    Collections.sort(triples);

    if (triples.size() < 500) {
      return triples.subList(0, triples.size());
    }
    return triples.subList(0, 500);
    // return triples;
  }

  public void close() throws IOException {
    ireader.close();
    directory.close();
  }

  public DirectoryReader getIreader() {
    return ireader;
  }

}
