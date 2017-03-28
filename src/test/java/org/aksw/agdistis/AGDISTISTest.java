package org.aksw.agdistis;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.AGDISTIS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.webapp.DisambiguationService;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AGDISTISTest {

  private final Logger log = LoggerFactory.getLogger(AGDISTISTest.class);

  @Test
  public void testUmlaute() throws InterruptedException, IOException {
    final String taisho = "Emperor Taishō";
    final String taishoURL = "http://dbpedia.org/resource/Emperor_Taishō";
    final String japan = "Japan";
    final String japanURL = "http://dbpedia.org/resource/Japan";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(taisho, taishoURL);
    correct.put(japan, japanURL);
    final String preAnnotatedText = "<entity>" + taisho + "</entity> was the 123rd Emperor of <entity>" + japan
        + "</entity>.";

    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = DisambiguationService.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
    }

  }

  @Test
  public void testMinimalExample() throws InterruptedException, IOException {
    final String obama = "Barack Obama";
    final String obamaURL = "http://dbpedia.org/resource/Barack_Obama";
    final String merkel = "Angela Merkel";
    final String merkelURL = "http://dbpedia.org/resource/Angela_Merkel";
    final String city = "Berlin";
    final String cityURL = "http://dbpedia.org/resource/Berlin";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(obama, obamaURL);
    correct.put(merkel, merkelURL);
    correct.put(city, cityURL);

    final String preAnnotatedText = "<entity>" + obama + "</entity> visits <entity>" + merkel + "</entity> in <entity>"
        + city + "</entity>.";

    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = DisambiguationService.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
    }
  }

  @Ignore
  @Test
  public void testContext() throws InterruptedException, IOException {
    final String angelina = "Angelina";
    final String angelinaURL = "http://dbpedia.org/resource/Angelina_Jolie";
    final String brad = "Brad";
    final String bradURL = "http://dbpedia.org/resource/Brad_Pitt";
    final String jon = "Jon";
    final String jonURL = "http://dbpedia.org/resource/Jon_Voight";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(angelina, angelinaURL);
    correct.put(jon, jonURL);
    correct.put(brad, bradURL);

    final String preAnnotatedText = "<entity>" + angelina + "</entity>, her father <entity>" + jon
        + "</entity>, and her partner <entity>" + brad + "</entity> never played together in the same movie.";

    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = DisambiguationService.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      Assert.assertEquals(correct.get(namedEntity.getLabel()), disambiguatedURL);
    }
  }

  @Test
  public void testCasingMatch() throws InterruptedException, IOException {
    final String entity = "ConforMIS";
    final String entityURL = "http://dbpedia.org/resource/fhai/45fdb6ae-966a-3024-885e-14b5aa8ecac0";
    final String entity2 = "GigSalad";
    final String entity2URL = "http://dbpedia.org/resource/fhai/09edd628-520e-38fd-b720-d9e6741c758b";
    final String entity3 = "eOasia";
    final String entity3URL = "http://dbpedia.org/resource/fhai/fbd807a2-9678-3df8-b57c-e0063d5bda2f";
    final String entity4 = "DermaDoctor";
    final String entity4URL = "http://dbpedia.org/resource/fhai/a6f4d86b-69f7-3c4c-a2b0-473fc0e80cd4";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(entity, entityURL);
    correct.put(entity2, entity2URL);
    correct.put(entity3, entity3URL);
    correct.put(entity4, entity4URL);

    final String preAnnotatedText = "<entity>" + entity
        + "</entity>, has sold more than 50,000 implants, each individually sized and shaped to fit each patient’s unique anatomy but <entity>"
        + entity2 + "</entity> is catching up together with <entity>" + entity3
        + "</entity> partners and the other <entity>" + entity4 + "</entity>.";

    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = DisambiguationService.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      Assert.assertEquals(correct.get(namedEntity.getLabel()), disambiguatedURL);
    }
  }

  @Test
  public void testPartialMatches1() throws InterruptedException, IOException {
    final String e1 = "Jorn Lyseggen";
    final String dis1 = "http://dbpedia.org/resource/Jørn_Lyseggen";
    final String e2 = "CEO";
    final String dis2 = "http://dbpedia.org/resource/CEO";
    final String e3 = "Meltwater";
    final String dis3 = "http://dbpedia.org/resource/Meltwater_Group";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(e1, dis1);
    correct.put(e2, dis2);
    correct.put(e3, dis3);

    final String preAnnotatedText = "<entity>" + e1 + "</entity> is the <entity>" + e2 + "</entity> of <entity>" + e3
        + "</entity>.";

    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = DisambiguationService.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      Assert.assertEquals(namedEntity.getNamedEntityUri(), disambiguatedURL);
    }
  }

  @Test
  public void testFromFile() throws InterruptedException, IOException {

    final StringWriter writer = new StringWriter();
    IOUtils.copy(AGDISTISTest.class.getResourceAsStream("clinton-test"), writer, "UTF-8");

    final String preAnnotatedText = writer.toString();
    long start = System.currentTimeMillis();
    final AGDISTIS agdistis = new AGDISTIS();
    log.info("AGDISTIS loaded in: {} msecs.", (System.currentTimeMillis() - start));
    final Document d = DisambiguationService.textToDocument(preAnnotatedText);
    start = System.currentTimeMillis();
    agdistis.run(d, null);
    log.info("Done in: {} msecs.", (System.currentTimeMillis() - start));

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
    }
  }

  @Test
  public void testPartialMatches2() throws InterruptedException, IOException {
    final String e1 = "Pendolino";
    final String dis1 = "http://dbpedia.org/resource/Pendolino";
    final String e2 = "Giugiaro";
    final String dis2 = "http://dbpedia.org/resource/Giorgetto_Giugiaro";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(e1, dis1);
    correct.put(e2, dis2);

    final String preAnnotatedText = "The <entity>" + e1 + "</entity> is a family of trains designed by <entity>" + e2
        + "</entity>.";

    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = DisambiguationService.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      Assert.assertEquals(namedEntity.getNamedEntityUri(), disambiguatedURL);
    }
  }

}
