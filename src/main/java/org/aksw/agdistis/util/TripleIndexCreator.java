package org.aksw.agdistis.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleParser;
import org.slf4j.LoggerFactory;

import info.aduna.io.FileUtil;

public class TripleIndexCreator {
  private static org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndexCreator.class);

  public static final String N_TRIPLES = "NTriples";
  public static final String TTL = "ttl";
  public static final String TSV = "tsv";
  public static final Version LUCENE_VERSION = Version.LUCENE_44;

  private Analyzer urlAnalyzer;
  private Analyzer literalAnalyzer;
  private DirectoryReader ireader;
  private IndexWriter iwriter;
  private MMapDirectory directory;

  public static void main(final String args[]) {
    if (args.length > 0) {
      log.error("TripleIndexCreator works without parameters. Please use agdistis.properties File");
      return;
    }
    try {
      log.info("For using DBpedia we suggest you download the following file: " + "labels_<LANG>.ttl, "
          + "redirects_transitive_<LANG>.ttl, " + "instance_types_<LANG>.ttl, " + "mappingbased_properties_<LANG>.ttl, "
          + "specific_mappingbased_properties_<LANG>.ttl," + "disambiguations_<LANG>.ttl." + ""
          + "Please download them into one folder and configure it in the agdistis.properties File."
          + "For further information have a look at our wiki: https://github.com/AKSW/AGDISTIS/wiki");

      final String index = AGDISTISConfiguration.INSTANCE.getMainIndexPath().toString();
      log.info("The index will be here: " + index);

      final String folder = AGDISTISConfiguration.INSTANCE.getIndexTTLPath().toString();
      log.info("Getting triple data from: " + folder);
      final List<File> listOfFiles = new ArrayList<File>();
      for (final File file : new File(folder).listFiles()) {
        if (FilenameUtils.getExtension(file.getAbsolutePath()).equals("ttl")) {
          listOfFiles.add(file);
        }
      }

      final String surfaceFormTSV = AGDISTISConfiguration.INSTANCE.getIndexSurfaceFormTSVPath().toString();
      log.info("Getting surface forms from: " + surfaceFormTSV);
      final File file = new File(surfaceFormTSV);
      if (file.exists()) {
        listOfFiles.add(file);
      }

      final String baseURI = AGDISTISConfiguration.INSTANCE.getBaseURI().toString();
      log.info("Setting Base URI to: " + baseURI);

      final TripleIndexCreator ic = new TripleIndexCreator();
      ic.createIndex(listOfFiles, index, baseURI);
      ic.close();
    } catch (final IOException e) {
      log.error("Error while creating index. Maybe the index is corrupt now.", e);
    }
  }

  public void createIndex(final List<File> files, final String idxDirectory, final String baseURI) {
    try {
      urlAnalyzer = new SimpleAnalyzer(LUCENE_VERSION);
      literalAnalyzer = new LiteralAnalyzer(LUCENE_VERSION);
      final Map<String, Analyzer> mapping = new HashMap<String, Analyzer>();
      mapping.put(TripleIndex.FIELD_NAME_SUBJECT, urlAnalyzer);
      mapping.put(TripleIndex.FIELD_NAME_PREDICATE, urlAnalyzer);
      mapping.put(TripleIndex.FIELD_NAME_OBJECT_URI, urlAnalyzer);
      mapping.put(TripleIndex.FIELD_NAME_OBJECT_LITERAL, literalAnalyzer);
      final PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(urlAnalyzer, mapping);

      final File indexDirectory = new File(idxDirectory);
      indexDirectory.mkdir();
      directory = new MMapDirectory(indexDirectory);
      final IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, perFieldAnalyzer);
      iwriter = new IndexWriter(directory, config);
      iwriter.commit();
      for (final File file : files) {
        final String type = FileUtil.getFileExtension(file.getName());
        if (type.equals(TTL)) {
          indexTTLFile(file, baseURI);
        }
        if (type.equals(TSV)) {
          indexTSVFile(file);
        }
        iwriter.commit();
      }
      iwriter.close();
      ireader = DirectoryReader.open(directory);
    } catch (final Exception e) {
      log.error("Error while creating TripleIndex.", e);
    }
  }

  private void indexTTLFile(final File file, final String baseURI)
      throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
    log.info("Start parsing: " + file);
    final RDFParser parser = new TurtleParser();
    final OnlineStatementHandler osh = new OnlineStatementHandler();
    parser.setRDFHandler(osh);
    parser.setStopAtFirstError(false);
    parser.parse(new FileReader(file), baseURI);
    log.info("Finished parsing: " + file);
  }

  private void indexTSVFile(final File file) throws IOException {
    log.info("Start parsing: " + file);
    final BufferedReader br = new BufferedReader(new FileReader(file));
    while (br.ready()) {
      final String[] line = br.readLine().split("\t");
      final String subject = line[0];
      for (int i = 1; i < line.length; ++i) {
        final String object = line[i];
        final Document doc = new Document();
        doc.add(new StringField(TripleIndex.FIELD_NAME_SUBJECT, subject, Store.YES));
        doc.add(new StringField(TripleIndex.FIELD_NAME_PREDICATE, "http://www.w3.org/2004/02/skos/core#altLabel",
            Store.YES));
        doc.add(new TextField(TripleIndex.FIELD_NAME_OBJECT_LITERAL, object, Store.YES));
        iwriter.addDocument(doc);
      }
    }
    br.close();
    log.info("Finished parsing: " + file);
  }

  private void addDocumentToIndex(final IndexWriter iwriter, final String subject, final String predicate,
      final String object, final boolean isUri) throws IOException {
    final Document doc = new Document();
    log.debug(subject + " " + predicate + " " + object);
    doc.add(new StringField(TripleIndex.FIELD_NAME_SUBJECT, subject, Store.YES));
    doc.add(new StringField(TripleIndex.FIELD_NAME_PREDICATE, predicate, Store.YES));
    if (isUri) {
      doc.add(new StringField(TripleIndex.FIELD_NAME_OBJECT_URI, object, Store.YES));
    } else {
      doc.add(new TextField(TripleIndex.FIELD_NAME_OBJECT_LITERAL, object, Store.YES));
    }
    iwriter.addDocument(doc);
  }

  public void close() throws IOException {
    if (ireader != null) {
      ireader.close();
    }
    if (directory != null) {
      directory.close();
    }
  }

  private class OnlineStatementHandler extends RDFHandlerBase {
    @Override
    public void handleStatement(final Statement st) {
      final String subject = st.getSubject().stringValue();
      final String predicate = st.getPredicate().stringValue();
      final String object = st.getObject().stringValue();
      try {
        addDocumentToIndex(iwriter, subject, predicate, object, st.getObject() instanceof URI);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }
}