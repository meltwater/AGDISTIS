package org.aksw.agdistis;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.AGDISTIS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.Utils;
import org.aksw.agdistis.webapp.DisambiguationService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.meltwater.fhai.kg.ned.agdistis.model.InputEntity;
import com.meltwater.fhai.kg.ned.agdistis.model.Occurrence;

public class AGDISTISTest {

  private static final Logger log = LoggerFactory.getLogger(AGDISTISTest.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testCamelCaseOrgSuffix() throws InterruptedException, IOException {

    final String text = "LabCorp has partnered with Walk-In Lab part of Walk-In LLC to provide blood test services across the country.";

    final HashMap<Occurrence, InputEntity> entities = Maps.newHashMap();

    Occurrence occ = new Occurrence(0, "LabCorp".length());
    final InputEntity labcorp = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, labcorp);

    occ = new Occurrence(27, "Walk-In Lab".length() + 27);
    final InputEntity walkinlab = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, walkinlab);

    occ = new Occurrence(47, "Walk-In LLC".length() + 47);
    final InputEntity walkinllc = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, walkinllc);

    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = Utils.textToDocument("testId", text, entities);
    agdistis.run(d, null);

    final String labcorpURL = "http://dbpedia.org/resource/fhai/7d29a76f-e767-435f-a38b-2ae55ce0084c";
    final String walkinlabURL = "Walk-In Lab";
    final String walkinllcURL = "Walk-In LLC";
    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(labcorp.getName(), labcorpURL);
    correct.put(walkinlab.getName(), walkinlabURL);
    correct.put(walkinllc.getName(), walkinllcURL);

    for (final NamedEntityInText namedEntity : d.getNamedEntitiesInText()) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      Assert.assertEquals(correct.get(namedEntity.getLabel()), disambiguatedURL);
    }
  }

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
    final Document d = Utils.textToDocument("testId", preAnnotatedText);
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
    final Document d = Utils.textToDocument("testId", preAnnotatedText);
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
  public void testMinimalExamplePlainTextWithEntities() throws InterruptedException, IOException {

    final String obamaURL = "http://dbpedia.org/resource/Barack_Obama";
    final String merkelURL = "http://dbpedia.org/resource/Angela_Merkel";
    final String berlinURL = "http://dbpedia.org/resource/Berlin";

    final HashMap<Occurrence, InputEntity> entities = Maps.newHashMap();

    Occurrence occ = new Occurrence(0, "Barack Obama".length());
    final InputEntity obama = new InputEntity("Barack Obama", "person", occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, obama);

    occ = new Occurrence(20, "Angela Merkel".length() + 20);
    final InputEntity merkel = new InputEntity("Angela Merkel", "person", occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, merkel);

    occ = new Occurrence(37, "Berlin".length() + 37);
    final InputEntity berlin = new InputEntity("Berlin", "city", occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, berlin);

    final String plainText = obama.getName() + " visits " + merkel.getName() + " in " + berlin.getName() + ".";
    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = Utils.textToDocument("testId", plainText, entities);
    agdistis.run(d, null);

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(obama.getName(), obamaURL);
    correct.put(merkel.getName(), merkelURL);
    correct.put(berlin.getName(), berlinURL);

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
    AGDISTISConfiguration.INSTANCE.setUseContext(true);
    final Document d = Utils.textToDocument("testId", preAnnotatedText);
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
  public void testEnconding() throws InterruptedException, IOException {
    final String text = "One hundred and fifty-one years after the first oil well was torpedoed (h/t Colonel E.A.L. Roberts), and the crude complex is blowing up again. Groundhog day has arrived early this year, as financial markets are stuck in a downward spiral, like a cat chasing its tail (um, on a downhill slope).\\n\\n.\\n\\nRelated: Does Buffett See A Bottom In Oil Prices\\n\\nEquity markets overnight have again sold off on fears of a slowing global economy, which has then tag-teamed with immediate oversupply to send crude prices lower, which is then raising fears of contagion in equity markets, and egging on further selling. Rinse and repeat.\\n\\nBut wait! We have had comments out of ECB President Mario Draghi (always sounds like a Formula 1 driver to me), who has signaled further quantitative easing is likely to be announced at the next ECB meeting. This has lent support to equities, but due to its negative impact on the euro, has provided further headwinds to a crude price rebound.\\n\\nIn terms of economic data flow, aside from the ECB decision to keep rates at 0.05 percent, the U.S. has seen disappointing releases from both weekly jobless claims and Philly Fed Manufacturing. Weekly claims came in at 293.000, the worst since last July, while Philly Fed has jumped in the conga line with other regional manufacturing indices to highlight deterioration from the sector.\\n\\nRelated: OPEC Still Sees Oil Markets Balancing This Year\\n\\nThe latest weekly inventory data is providing no modicum of support, although it never seemed likely to given the fact that current U.S. refinery maintenance is taking well over 1 million barrels per day of crude oil demand out of the U.S. market. Last night\u00E2\u20AC\u2122s API report is pointing to a solid build from today\u00E2\u20AC\u2122s EIA release, as it revealed a 4.6 million barrel build to crude stocks.\\n\\nIt was, however, the 4.7 million barrel build to gasoline stocks which provided the sucker punch. While the EIA looks to be underestimating gasoline demand (a knock-on effect of it overestimating exports), another large build to gasoline stocks is tough for the market to take.\\n\\nAs prices plumb the depths of twenty-dollardom, this graphic (hark, above left) illustrates how key petrostates now see oil prices well below the level needed to balance their budgets. Libya remains in the biggest trouble, some $180 a barrel adrift from meeting its budget due to the ongoing conflict with the Islamic State (oil infrastructure near the port of Ras Lanuf was attacked last night, with the promise of further attacks elsewhere in the coming days\u00E2\u20AC\u00A6and the oil market just shrugged\u00E2\u20AC\u00A6).\\n\\nRelated:The Condensate Con: How Real Is The Oil Glut?\\n\\nWhile last year the key theme in the crude market was oversupply, this year we are already seeing a new theme coming to the forefront: that of weakening oil demand growth. We are starkly seeing this in our ClipperData, as Chinese crude imports continue to head lower. Waterborne imports for this month are on target to reach their lowest volume since mid-2013, with monthly volumes set to be below year-ago levels for the fourth consecutive month.\\n\\n.\\n\\nThe chart below puts emerging markets in context somewhat from an economic standpoint. Despite rapid economic growth in the past few decades, BRIC\u00E2\u20AC\u2122s share of global GDP remains considerably adrift of G-7\u00E2\u20AC\u2122s share (G7 = U.S., Japan, Germany, U.K., France, Italy, Canada):\\n\\n.\\n\\n(Click to enlarge)\\n\\nFinally, a barrel of oil in Canada now costs five cauliflowers, while some crude in North Dakota is being given away. It is nutty times we live in.\\n\\nBy Matt Smith\\n\\nMore Top Reads From Oilprice.com:\\n\\nOil Sold for -$0.50 per Barrel. A Negative Price!\\n$329 Billion Invested in Clean Energy in 2015\\nOman Offers to Slash Oil Production If OPEC Follows Suit";
    System.out.println(text.substring(1768, 1771));
    int index = 0;
    while (index >= 0) {
      final int found = text.indexOf("EIA", index + 1);
      System.out.println(found + " " + (found + StringUtils.length("EIA")));
      index = found;
    }
  }

  @Test
  public void testCasingMatch() throws InterruptedException, IOException {
    final String entity = "ConforMIS";
    final String entityURL = "http://dbpedia.org/resource/fhai/cde6bf96-cda8-45ef-9b10-10b5613b2ced";
    final String entity2 = "GigSalad";
    final String entity2URL = "http://dbpedia.org/resource/fhai/eb561d3c-b836-40ae-adba-de9dbdc58893";
    final String entity3 = "eOasia";
    final String entity3URL = "http://dbpedia.org/resource/fhai/e81bf6ac-c538-4630-810b-e7a7d458ae02";
    final String entity4 = "DermaDoctor";
    final String entity4URL = "http://dbpedia.org/resource/fhai/1e197314-b903-414b-ac6d-95a5711c5e8e";

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
    final Document d = Utils.textToDocument("testId", preAnnotatedText);
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
    final Document d = Utils.textToDocument("testId", preAnnotatedText);
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
    final Document d = Utils.textToDocument("testId", preAnnotatedText);
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
    final Document d = Utils.textToDocument("testId", preAnnotatedText);
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
  public void testSingle() throws InterruptedException, IOException {
    final String preAnnotatedText = "The <entity>Charlotte Hornets</entity> won the last match.";

    final AGDISTIS agdistis = new AGDISTIS();
    final Document d = Utils.textToDocument("testId", preAnnotatedText);
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

  @Test
  public void testJsonOutput() throws InterruptedException, IOException {

    final String text = "LabCorp has partnered with Walk-In Lab part of Walk-In LLC to provide blood test services across the country.";

    final HashMap<Occurrence, InputEntity> entities = Maps.newHashMap();

    Occurrence occ = new Occurrence(0, "LabCorp".length());
    final InputEntity labcorp = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, labcorp);

    occ = new Occurrence(27, "Walk-In Lab".length() + 27);
    final InputEntity walkinlab = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, walkinlab);

    occ = new Occurrence(47, "Walk-In LLC".length() + 47);
    final InputEntity walkinllc = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, walkinllc);

    final DisambiguationService service = new DisambiguationService();
    final String agdistisOutput = service.standardAG("testId", text, entities);
    final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    log.info(IOUtils.LINE_SEPARATOR + writer.writeValueAsString(mapper.readTree(agdistisOutput)));
  }

  @Test
  public void testApple() throws InterruptedException, IOException {

    final String text = "The Taiwan-based company best known for manufacturing Apple products insists that its new plant won't damage the environment.";

    final HashMap<Occurrence, InputEntity> entities = Maps.newHashMap();

    final Occurrence occ = new Occurrence(54, 54 + "Apple".length());
    final InputEntity apple = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, apple);

    final DisambiguationService service = new DisambiguationService();
    final String agdistisOutput = service.standardAG("testId", text, entities);
    final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    log.info(IOUtils.LINE_SEPARATOR + writer.writeValueAsString(mapper.readTree(agdistisOutput)));
  }

  @Test
  public void testUber() throws InterruptedException, IOException {

    final String text = "Uber drivers, street vendors, fast-food workers and union activists arrived downtown by the busload on Monday to participate in a boisterous march.";

    final HashMap<Occurrence, InputEntity> entities = Maps.newHashMap();

    final Occurrence occ = new Occurrence(0, "Uber".length());
    final InputEntity uber = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, uber);

    final DisambiguationService service = new DisambiguationService();
    final String agdistisOutput = service.standardAG("testId", text, entities);
    final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    log.info(IOUtils.LINE_SEPARATOR + writer.writeValueAsString(mapper.readTree(agdistisOutput)));
  }

  @Test
  public void testQBD() throws InterruptedException, IOException {

    final String text = "Quick Business Deposit is a company.";

    final HashMap<Occurrence, InputEntity> entities = Maps.newHashMap();

    final Occurrence occ = new Occurrence(0, "Quick Business Deposit".length());
    final InputEntity qbd = new InputEntity(text.substring(occ.getStartOffset(), occ.getEndOffset()), "ORG",
        occ.getStartOffset(), occ.getEndOffset());
    entities.put(occ, qbd);

    final DisambiguationService service = new DisambiguationService();
    final String agdistisOutput = service.standardAG("testId", text, entities);
    final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    log.info(IOUtils.LINE_SEPARATOR + writer.writeValueAsString(mapper.readTree(agdistisOutput)));
  }

}
