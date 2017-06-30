package org.aksw.agdistis.webapp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.aksw.agdistis.algorithm.AGDISTIS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.model.CandidatesScore;
import org.aksw.agdistis.util.NIFParser;
import org.aksw.agdistis.util.Utils;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meltwater.fhai.kg.ned.agdistis.model.InputEntity;
import com.meltwater.fhai.kg.ned.agdistis.model.Occurrence;

public class DisambiguationService extends ServerResource {

  private static Logger LOGGER = LoggerFactory.getLogger(DisambiguationService.class);
  private final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
  private final TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
  private final NIFParser nifParser = new NIFParser();
  private final AGDISTIS agdistis = new AGDISTIS();

  @Post
  public String postText(final Representation entity) throws IOException, Exception {

    LOGGER.info("Start working on Request for AGDISTIS");
    String result = "";
    String text = "";
    String type = "";
    final String documentId = UUID.randomUUID().toString();
    final InputStream input = entity.getStream();
    // here the inputStream is duplicated due to it can be read only once.
    // Therefore, we do it for checking if the input is from gerbil or not.
    final byte[] byteArray = IOUtils.toByteArray(input);
    final InputStream input1 = new ByteArrayInputStream(byteArray);
    final InputStream input2 = new ByteArrayInputStream(byteArray);

    final String string = IOUtils.toString(input1, "UTF-8");
    // Parse the given representation and retrieve data
    final Form form = new Form(string);
    text = form.getFirstValue("text");
    type = form.getFirstValue("type");
    LOGGER.info("text: " + text);
    LOGGER.info("type: " + type);

    if (text == null) {
      result = NIFGerbil(input2, agdistis); // This part is created to
      // work
      // along with GERBIL, because
      // GERBIL only sends the NIF
      // files without taking care of
      // more than one parameter. So,
      // GERBIL is not capable to send
      // the nif in the text parameter
      // making
      // AGDISTIS?type=nif&text= not
      // work.
      return result;
    }
    if (type == null) {
      type = "agdistis";
    }

    if (type.equals("agdistis")) {
      return standardAG(documentId, text); // This type is the standard
      // and in case the user
      // doesn't send the type
      // parameter, it is
      // considered as the main
      // one(e.g
      // AGDISTIS?type=agdistis&text=<entity>Barack
      // Obama</entity>).

    } else if (type.equals("nif")) {
      return NIFType(documentId, text); // This type is for AGDISTIS
      // works beyond the GERBIL, this
      // part is in case of user wants
      // to check just a certain NIF
      // file(e.g
      // AGDISTIS?type=nif&text=@prefix....)

    } else if (type.equals("candidates")) {
      return candidateType(documentId, text); // Here is to let us know
      // about all candidates
      // for each mention and
      // its respective
      // HITS/PageRank score.
    } else {
      return "ERROR";
    }
  }

  public String NIFGerbil(final InputStream input, final AGDISTIS agdistis) throws IOException {
    org.aksw.gerbil.transfer.nif.Document document;
    String nifDocument = "";
    String textWithMentions = "";
    final List<MeaningSpan> annotations = new ArrayList<>();
    try {
      document = parser.getDocumentFromNIFStream(input);
      LOGGER.info("NIF file coming from GERBIL");
      textWithMentions = nifParser.createTextWithMentions(document.getText(), document.getMarkings(Span.class));
      final Document d = Utils.textToDocument(document.getDocumentURI(), textWithMentions);
      agdistis.run(d, null);
      for (final NamedEntityInText namedEntity : d.getNamedEntitiesInText()) {
        final String disambiguatedURL = namedEntity.getNamedEntityUri();

        if (disambiguatedURL == null) {
          annotations.add(new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(),
              URLDecoder.decode("http://aksw.org/notInWiki/" + namedEntity.getSingleWordLabel(), "UTF-8")));
        } else {
          annotations.add(new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(),
              URLDecoder.decode(namedEntity.getNamedEntityUri(), "UTF-8")));
        }
      }
      document.setMarkings(new ArrayList<Marking>(annotations));
      LOGGER.debug("Result: " + document.toString());
      nifDocument = creator.getDocumentAsNIFString(document);
      LOGGER.debug(nifDocument);

    } catch (final Exception e) {
      LOGGER.error("Exception while reading request.", e);
      return "";
    }

    return nifDocument;
  }

  public String standardAG(final String documentId, final String text,
      final Map<Occurrence, InputEntity> offsetEntityMap) {
    final Document d = Utils.textToDocument(documentId, text, offsetEntityMap);
    return runAG(d);
  }

  public String standardAG(final String documentId, final String text) {
    final Document d = Utils.textToDocument(documentId, text);
    return runAG(d);
  }

  public String runAG(final Document d) {

    agdistis.run(d, null);

    final JSONArray arr = new JSONArray();
    for (final NamedEntityInText namedEntity : d.getNamedEntitiesInText()) {
      final JSONObject obj = new JSONObject();
      obj.put("namedEntity", namedEntity.getLabel());
      obj.put("start", namedEntity.getStartPos());
      obj.put("offset", namedEntity.getLength());
      obj.put("disambiguatedURL", namedEntity.getNamedEntityUri());

      final JSONArray nedTypesArray = new JSONArray();
      for (final String type : namedEntity.getDisambiguatedTypes()) {
        nedTypesArray.add(type);
      }
      obj.put("disambiguatedTypes", nedTypesArray);
      arr.add(obj);
    }
    LOGGER.info("\t" + arr.toString());
    LOGGER.info("Finished Request");
    return arr.toString();
  }

  public String NIFType(final String documentId, final String text) throws IOException {
    org.aksw.gerbil.transfer.nif.Document document = null;
    String nifDocument = "";
    final NIFParser nifParser = new NIFParser();
    String textWithMentions = "";
    final List<MeaningSpan> annotations = new ArrayList<>();

    try {
      document = parser.getDocumentFromNIFString(text);
      LOGGER.debug("Request: " + document.toString());
      textWithMentions = nifParser.createTextWithMentions(document.getText(), document.getMarkings(Span.class));
      final Document d = Utils.textToDocument(documentId, textWithMentions);
      agdistis.run(d, null);
      for (final NamedEntityInText namedEntity : d.getNamedEntitiesInText()) {
        final String disambiguatedURL = namedEntity.getNamedEntityUri();

        if (disambiguatedURL == null) {
          annotations.add(new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(), new HashSet<String>()));
        } else {
          annotations.add(new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(),
              URLDecoder.decode(disambiguatedURL, "UTF-8")));
        }
      }
      document.setMarkings(new ArrayList<Marking>(annotations));
      LOGGER.debug("Result: " + document.toString());
      nifDocument = creator.getDocumentAsNIFString(document);
    } catch (final Exception e) {
      LOGGER.error("Exception while reading request.", e);
      return "";
    }
    return nifDocument;
  }

  public String candidateType(final String documentId, final String text) {
    final JSONArray arr = new org.json.simple.JSONArray();
    final Document d = Utils.textToDocument(documentId, text);
    final Map<NamedEntityInText, List<CandidatesScore>> candidatesPerNE = new HashMap<>();
    agdistis.run(d, candidatesPerNE);
    for (final NamedEntityInText namedEntity : candidatesPerNE.keySet()) {
      final List<CandidatesScore> candidates = candidatesPerNE.get(namedEntity);
      final JSONObject obj = new JSONObject();
      obj.put("namedEntity", namedEntity.getLabel());
      obj.put("Candidates", candidates.toString());
      arr.add(obj);
    }

    LOGGER.info("\t" + arr.toString());
    LOGGER.info("Finished Request");
    return arr.toString();
  }
}
