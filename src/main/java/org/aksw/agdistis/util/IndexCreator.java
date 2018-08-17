package org.aksw.agdistis.util;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
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



/**
 * To be used only when the following resources are available
 * 1. pageIds
 * 2. anchorTextFilePath
 * @author omkar
 *
 */

public class IndexCreator {
    
    private static org.slf4j.Logger log = LoggerFactory.getLogger(IndexCreator.class);

    public static final String N_TRIPLES = "NTriples";
    public static final String TTL = "ttl";
    public static final Version LUCENE_VERSION = Version.LUCENE_4_9;

    private Analyzer urlAnalyzer;
    private Analyzer literalAnalyzer;
    private DirectoryReader ireader;
    private IndexWriter iwriter;
    private MMapDirectory directory;
    
    
    private Map<String, Integer> resourceToId;
    private static String baseURI;
    private static final String projectName = "fhai";
    private static final String anchorTextUri = "<http://dbpedia.org/ontology/anchorText>";
    private Map<Integer, Map<String,Double>> idToAnchorTextToProb;
    
    
    
    public static void main(String[] args){
        
        final IndexCreator obj = new IndexCreator();
        try {
            final String index = AGDISTISConfiguration.INSTANCE.getMainIndexPath().toString();
            log.info("The index will be here: " + index);
    
            
            final String pageIds = AGDISTISConfiguration.INSTANCE.getPageIdsFilePath().toString();
            if(pageIds != null && !pageIds.isEmpty()){
                log.info("Reading page id information from" +pageIds);
                obj.readPageIds(pageIds);
            }
            
            final String anchorTextFilePath = AGDISTISConfiguration.INSTANCE.getAnchorTextsFilePath().toString();
            if(anchorTextFilePath != null && !anchorTextFilePath.isEmpty()){
               log.info("Reading anchor texts from "+anchorTextFilePath);
               obj.readAnchorTexts(anchorTextFilePath);
            }
           
            final String folder = AGDISTISConfiguration.INSTANCE.getIndexTTLPath().toString();
            log.info("Getting triple data from: " + folder);
            final List<File> listOfFiles = new ArrayList<File>();
            for (final File file : new File(folder).listFiles()) {
              if (FilenameUtils.getExtension(file.getAbsolutePath()).equals("ttl")) {
                listOfFiles.add(file);
              }
            }
            
            final String baseURI = AGDISTISConfiguration.INSTANCE.getBaseURI().toString();
            log.info("Setting Base URI to: " + baseURI);
    
            final IndexCreator ic = new IndexCreator();
            ic.createIndex(listOfFiles, index, baseURI);
            ic.close();
        } catch (IOException e) {
            log.error("Error while creating index. Maybe the index is corrupt now.", e);
        }
    }
    
    
    private void readPageIds(String pageIdsFilePath) throws IOException{
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(pageIdsFilePath), "UTF-8"))  ;
        int lineCnt = 0;
        while(true){
            lineCnt++;
            if (lineCnt % 10000 == 0) System.out.print(".");
            if (lineCnt % 1000000 == 0) {
                System.out.println(lineCnt);
            }
            
            String line = lnr.readLine();
            if (line == null) break;
            String[] parts = line.split("\t");
            int id = Integer.parseInt(parts[0]);
            String title = parts[1];
            resourceToId.put(title, id);
        }
        lnr.close();
        log.info(resourceToId.size() + " pages read");
    }
    
    private void readAnchorTexts(String anchorTextFilePath) throws IOException{
        idToAnchorTextToProb = new LinkedHashMap<Integer, Map<String,Double>>();
        
        InputStream is = new FileInputStream(anchorTextFilePath);
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        LineNumberReader lnr = new LineNumberReader(isr);
        System.out.println("Processing " + anchorTextFilePath + " ...");
        int lineCnt = 0;
        while (true) {
            lineCnt++;
            if (lineCnt % 10000 == 0) System.out.print(".");
            if (lineCnt % 1000000 == 0) {
                System.out.println(lineCnt);
            }
            
            String line = lnr.readLine();
            if (line == null) break;
            String[] parts = line.split("\t");
            double total = Double.parseDouble(parts[0]);
            String anchorText = parts[1];
            if(anchorText == null || anchorText.trim().isEmpty()){
                System.out.println("empty anchor text ...");
                continue;
            }
            for(int i=2; i < parts.length; i=i+2){
                int id = Integer.parseInt(parts[i]);
                int count = Integer.parseInt(parts[i+1]);
                if(id == -1)continue;
                Map<String, Double> at = idToAnchorTextToProb.get(id);
                if(null == at){
                    at = new HashMap<String, Double>();
                }
                at.put(anchorText, (count/total));
                idToAnchorTextToProb.put(id, at);
            }
        }
        lnr.close();
    }

    public void createIndex(final List<File> files, final String idxDirectory, final String baseURI) {
        try {
          urlAnalyzer = new SimpleAnalyzer(LUCENE_VERSION);
          literalAnalyzer = new LiteralAnalyzer(LUCENE_VERSION);
          final Map<String, Analyzer> mapping = new HashMap<String, Analyzer>();
          mapping.put(CandidateSearcher.FIELD_NAME_SUBJECT, urlAnalyzer);
          mapping.put(CandidateSearcher.FIELD_NAME_PREDICATE, urlAnalyzer);
          mapping.put(CandidateSearcher.FIELD_NAME_OBJECT_URI, urlAnalyzer);
          mapping.put(CandidateSearcher.FIELD_NAME_OBJECT_LITERAL, literalAnalyzer);
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
      
      
      private void addDocumentToIndex(final IndexWriter iwriter, final String subject, final String predicate,
              final String object, final boolean isUri) throws IOException {
            final Document doc = new Document();
            log.debug(subject + " " + predicate + " " + object);
            
            Integer id = titleToId(subject);
            if(id == null){
                // This should not happen
                log.error("id is null for "+subject);
                throw new RuntimeException("id is null for "+subject);
            }
            doc.add(new IntField(CandidateSearcher.FIELD_NAME_ID, id, Store.YES));
            
            doc.add(new StringField(CandidateSearcher.FIELD_NAME_SUBJECT, subject, Store.YES));
            doc.add(new StringField(CandidateSearcher.FIELD_NAME_PREDICATE, predicate, Store.YES));
            if (isUri) {
              doc.add(new StringField(CandidateSearcher.FIELD_NAME_OBJECT_URI, object, Store.YES));
            } else {
              doc.add(new TextField(CandidateSearcher.FIELD_NAME_OBJECT_LITERAL, object, Store.YES));
            }
            
            // if the predicate carries anchorText <http://dbpedia.org/ontology/anchorText>
            double anchorProb = 0.0;
            if(predicate.equals(anchorTextUri)){
                String anchorText = object.substring(1, object.lastIndexOf("\""));
                anchorProb = getAnchorProb(id,anchorText);
                doc.add(new DoubleField(CandidateSearcher.FIELD_NAME_ANCHOR_PROB, anchorProb, Store.YES));
            }
            iwriter.addDocument(doc);
      }
      
      
      private double getAnchorProb(int id, String anchorText) {
        return idToAnchorTextToProb.get(id).get(anchorText);
    }


    private Integer titleToId(String subject) {
          int baseUriIndex = subject.indexOf(baseURI);
          String title = subject.substring(baseUriIndex + baseURI.length());
          if(title.startsWith(projectName)){
              title = title.substring(projectName.length());
          }
          return resourceToId.get(title);
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