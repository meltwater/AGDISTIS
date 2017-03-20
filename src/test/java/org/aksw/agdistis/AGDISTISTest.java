package org.aksw.agdistis;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.AGDISTIS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.webapp.GetDisambiguation;
import org.junit.Test;

public class AGDISTISTest {

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
    final Document d = GetDisambiguation.textToDocument(preAnnotatedText);
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
    final Document d = GetDisambiguation.textToDocument(preAnnotatedText);
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
    final Document d = GetDisambiguation.textToDocument(preAnnotatedText);
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
  public void testContext2() throws InterruptedException, IOException {
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
    final Document d = GetDisambiguation.textToDocument(preAnnotatedText);
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
    }
  }

}
