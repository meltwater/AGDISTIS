package org.aksw.agdistis.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class TripleIndex {

  private static final Version LUCENE44 = Version.LUCENE_44;

  private final Logger log = LoggerFactory.getLogger(TripleIndex.class);

  public static final String FIELD_NAME_SUBJECT = "subject";
  public static final String FIELD_NAME_PREDICATE = "predicate";
  public static final String FIELD_NAME_OBJECT_URI = "object_uri";
  public static final String FIELD_NAME_OBJECT_LITERAL = "object_literal";
  private static final String[] _LUCENE_KEYWORDS = new String[] { "AND", "OR", "NOT", "TO" };
  private static final String[] _LUCENE_KEYWORDS_REPLACEMENTS = new String[] { "\\AND", "\\OR", "\\NOT", "\\TO" };
  // public static final String FIELD_URI_COUNT = "uri_counts";
  public static final String FIELD_FREQ = "freq";

  private final int defaultMaxNumberOfDocsRetrievedFromIndex = 100;

  private final Directory directory;
  private final IndexSearcher isearcher;
  private final DirectoryReader ireader;
  private final UrlValidator urlValidator;
  private final Cache<BooleanQuery, List<Triple>> cache;
  StringUtils isInt = new StringUtils();

  public TripleIndex() throws IOException {
    final String index = AGDISTISConfiguration.INSTANCE.getMainIndexPath().toString();
    log.info("The index will be loaded from: " + index);

    directory = new MMapDirectory(new File(index));
    final long start = System.currentTimeMillis();
    ireader = DirectoryReader.open(directory);
    log.debug("Index loaded in {} msec", System.currentTimeMillis() - start);
    isearcher = new IndexSearcher(ireader);
    urlValidator = new UrlValidator();

    cache = CacheBuilder.newBuilder().weakKeys().softValues().maximumSize(50000).build();
  }

  public List<Triple> search(final String subject, final String predicate, final String object) {
    return search(subject, predicate, object, defaultMaxNumberOfDocsRetrievedFromIndex);
  }

  public List<Triple> search(final String subject, final String predicate, String object,
      final int maxNumberOfResults) {
    final BooleanQuery bq = new BooleanQuery();
    List<Triple> triples;

    try {
      if ((subject != null) && subject.equals("http://aksw.org/notInWiki")) {
        log.error(
            "A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
      }
      if (subject != null) {
        final Query tq = new TermQuery(new Term(FIELD_NAME_SUBJECT, subject));
        bq.add(tq, BooleanClause.Occur.MUST);
      }
      if (predicate != null) {
        final Query tq = new TermQuery(new Term(FIELD_NAME_PREDICATE, predicate));
        bq.add(tq, BooleanClause.Occur.MUST);
      }

      // if (object != null) {
      // Query tq = new TermQuery(new Term(FIELD_NAME_OBJECT_LITERAL,
      // object));
      // bq.add(tq, BooleanClause.Occur.MUST);
      // }
      if ((object != null) && !StringUtils.isBlank(object)) {
        Query q = null;
        if (urlValidator.isValid(object)) {

          q = new TermQuery(new Term(FIELD_NAME_OBJECT_URI, object));
          bq.add(q, BooleanClause.Occur.MUST);

        } else if (StringUtils.isNumeric(object)) {
          // System.out.println("here numeric");
          final int tempInt = Integer.parseInt(object);
          final BytesRef bytes = new BytesRef(NumericUtils.BUF_SIZE_INT);
          NumericUtils.intToPrefixCoded(tempInt, 0, bytes);
          q = new TermQuery(new Term(FIELD_NAME_OBJECT_LITERAL, bytes.utf8ToString()));
          bq.add(q, BooleanClause.Occur.MUST);

        }

        else {
          final Analyzer analyzer = new LiteralAnalyzer(LUCENE44);
          final QueryParser parser = new QueryParser(LUCENE44, FIELD_NAME_OBJECT_LITERAL, analyzer);
          parser.setDefaultOperator(QueryParser.Operator.AND);
          q = parser.parse(QueryParserBase
              .escape(object = StringUtils.replaceEach(object, _LUCENE_KEYWORDS, _LUCENE_KEYWORDS_REPLACEMENTS)));
          bq.add(q, BooleanClause.Occur.MUST);
        }
      }

      // use the cache
      if (null == (triples = cache.getIfPresent(bq))) {
        triples = getFromIndex(maxNumberOfResults, bq);
        cache.put(bq, triples);
      }
      return triples;
    } catch (final IOException ioe) {
      log.error("I/O exception occurred while reading from the index. Corrupt?. StackTrace {}",
          ExceptionUtils.getStackTrace(ioe), subject);
      return Lists.newLinkedList();
    } catch (final ParseException pe) {
      log.error("Unable to parse the object from the triple <{},{},{}>.", subject, predicate, object);
      return Lists.newLinkedList();
    }

  }

  private List<Triple> getFromIndex(final int maxNumberOfResults, final BooleanQuery bq) throws IOException {
    log.trace("start asking index...");
    final TopScoreDocCollector collector = TopScoreDocCollector.create(maxNumberOfResults, true);
    // Similarity BM25Similarity = new BM25Similarity();
    // isearcher.setSimilarity(BM25Similarity);
    isearcher.search(bq, collector);
    final ScoreDoc[] hits = collector.topDocs().scoreDocs;

    final List<Triple> triples = new LinkedList<Triple>();
    String s, p, o;
    for (final ScoreDoc hit : hits) {
      final Document hitDoc = isearcher.doc(hit.doc);
      s = hitDoc.get(FIELD_NAME_SUBJECT);
      p = hitDoc.get(FIELD_NAME_PREDICATE);
      o = hitDoc.get(FIELD_NAME_OBJECT_URI);
      if (o == null) {
        o = hitDoc.get(FIELD_NAME_OBJECT_LITERAL);
      }
      final Triple triple = new Triple(s, p, o);
      triples.add(triple);
    }
    log.trace("finished asking index...");
    return triples;
  }

  public void close() throws IOException {
    ireader.close();
    directory.close();
  }

  public DirectoryReader getIreader() {
    return ireader;
  }

}
