package org.aksw.agdistis;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.search.spell.NGramDistance;
import org.apache.lucene.search.spell.StringDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * A (singleton) configuration class for AGDISTIS.
 * @author <a href="mailto:giorgio.orsi@meltwater.com">Giorgio Orsi</a>
 **/
public class AGDISTISConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AGDISTISConfiguration.class);
  private static final Path _PATH_WORKING_DIR = Paths.get(System.getProperty("user.dir"));
  private static final String _AGDISTIS_PROPERTY_FILE = "agdistis.properties";
  private static final Path _PATH_DEFAULT_CONFIG_FILE = Paths.get("/config", _AGDISTIS_PROPERTY_FILE);

  private static Map<ConfigProperty, Object> CONFIGURATION;

  // Enables lazy initialization of the singleton configuration
  public final static AGDISTISConfiguration INSTANCE = new AGDISTISConfiguration();

  // The constructor is private to force the singleton behaviour.
  private AGDISTISConfiguration() {

    CONFIGURATION = Maps.newHashMap();

    // Default configuration.
    setMainIndexPath(Paths.get("index"));
    setIndexByContextPath(Paths.get("index_bycontext"));
    setNodeType(URI.create("http://dbpedia.org/resource/"));
    setEdgeType(URI.create("http://dbpedia.org/ontology/"));
    setBaseURI(URI.create("http://dbpedia.org"));
    setMaxCandidateLookups(100);
    setMaxAcronymLookups(2);
    setMaxConnectionLookups(20);
    setCandidateCacheSize(500);
    setDisambiguationPageCacheSize(500);
    setTripleIndexCacheSize(1500);
    setNGramDistance(3);
    setCandidatePruningMetric("org.apache.lucene.search.spell.NGramDistance");
    setSemanticDepth(2);
    setCandidatePruningThreshold(.87);
    setHeuristicExpansion(true);
    setPreDisambiguationWhiteListPath(Paths.get("/config/pre-disambiguation-whitelist.txt"));
    setPostDisambiguationWhiteListPath(Paths.get("/config/post-disambiguation-whitelist.txt"));
    setCorporationAffixesPath(Paths.get("/config/corporationAffixes.txt"));
    setUsePopularity(false);
    setAlgorithm(Algorithm.HITS);
    setUseContext(false);
    setUseAcronym(true);
    setUseSurfaceForms(false);
    setUseCommonEntities(false);
    setForceNER2NEDMapping(true);
    final Map<String, String> ner2ned = Maps.newHashMap();
    ner2ned.put("Person", "PER");
    ner2ned.put("Organization", "ORG");
    setNER2NEDMapping(ner2ned);
//    setIndexTTLPath(Paths.get("data/en"));
    setIndexSurfaceFormTSVPath(Paths.get("data/en/surface/en_surface_forms.tsv"));
    //setPageIdsFilePath(Paths.get("data/en/ids/kg_page_ids-merged.txt"));
    //setAnchorTextsFilePath(Paths.get("data/en/anchor/kg_anchor_stats-remapped.txt"));
    //setInLinkFilePath(Paths.get("data/en/inlinks/kg_graph_in-merged-remapped.txt"));

    // Attempt to load the configuration file. Values in the file-based configuration override the default
    // configuration.
    try {

      final Properties prop = new Properties();

      // Load the meta-properties.
      prop.load(AGDISTISConfiguration.class.getResourceAsStream(_AGDISTIS_PROPERTY_FILE));

      // Load the AGDISTIS properties.
      prop.load(AGDISTISConfiguration.class.getResourceAsStream(_PATH_DEFAULT_CONFIG_FILE.toString()));

      // Override properties with system properties provided via command line (if present).
      prop.putAll(System.getProperties());

      // The AGDISTIS and schema versions must be there
      setAGDISTISVersion(prop.getProperty(ConfigProperty.AGDISTIS_VERSION.getPropertyName()));
      setSchemaVersion(prop.getProperty(ConfigProperty.SCHEMA_VERSION.getPropertyName()));

      // override default properties.
      if (prop.containsKey(ConfigProperty.MAIN_INDEX_PATH.getPropertyName())) {
        setMainIndexPath(Paths.get(prop.getProperty(ConfigProperty.MAIN_INDEX_PATH.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.INDEX_BY_CONTEXT_PATH.getPropertyName())) {
        setIndexByContextPath(Paths.get(prop.getProperty(ConfigProperty.INDEX_BY_CONTEXT_PATH.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.NODE_TYPE.getPropertyName())) {
        setNodeType(URI.create(prop.getProperty(ConfigProperty.NODE_TYPE.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.EDGE_TYPE.getPropertyName())) {
        setEdgeType(URI.create(prop.getProperty(ConfigProperty.EDGE_TYPE.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.BASE_URI.getPropertyName())) {
        setBaseURI(URI.create(prop.getProperty(ConfigProperty.BASE_URI.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.NGRAM_DISTANCE.getPropertyName())) {
        setNGramDistance(Integer.parseInt(prop.getProperty(ConfigProperty.NGRAM_DISTANCE.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.MAX_CANDIDATE_LOOKUPS.getPropertyName())) {
        setMaxCandidateLookups(
            Integer.parseInt(prop.getProperty(ConfigProperty.MAX_CANDIDATE_LOOKUPS.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.MAX_ACRONYM_LOOKUPS.getPropertyName())) {
        setMaxAcronymLookups(Integer.parseInt(prop.getProperty(ConfigProperty.MAX_ACRONYM_LOOKUPS.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.MAX_CONNECTION_LOOKUPS.getPropertyName())) {
        setMaxConnectionLookups(
            Integer.parseInt(prop.getProperty(ConfigProperty.MAX_CONNECTION_LOOKUPS.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.SEMANTIC_DEPTH.getPropertyName())) {
        setSemanticDepth(Integer.parseInt(prop.getProperty(ConfigProperty.SEMANTIC_DEPTH.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.CANDIDATE_PRUNING_METRIC.getPropertyName())) {
        setCandidatePruningMetric(prop.getProperty(ConfigProperty.CANDIDATE_PRUNING_METRIC.getPropertyName()));
      }
      if (prop.containsKey(ConfigProperty.CANDIDATE_PRUNING_THRESHOLD.getPropertyName())) {
        setCandidatePruningThreshold(
            Double.parseDouble(prop.getProperty(ConfigProperty.CANDIDATE_PRUNING_THRESHOLD.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.HEURISTIC_EXPANSION.getPropertyName())) {
        setHeuristicExpansion(
            Boolean.parseBoolean(prop.getProperty(ConfigProperty.HEURISTIC_EXPANSION.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.PRE_DISAMBIGUATION_WHITE_LIST_PATH.getPropertyName())) {
        setPreDisambiguationWhiteListPath(
            Paths.get(prop.getProperty(ConfigProperty.PRE_DISAMBIGUATION_WHITE_LIST_PATH.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.POST_DISAMBIGUATION_WHITE_LIST_PATH.getPropertyName())) {
        setPostDisambiguationWhiteListPath(
            Paths.get(prop.getProperty(ConfigProperty.POST_DISAMBIGUATION_WHITE_LIST_PATH.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.CORPORATION_AFFIXES_PATH.getPropertyName())) {
        setCorporationAffixesPath(
            Paths.get(prop.getProperty(ConfigProperty.CORPORATION_AFFIXES_PATH.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.USE_POPULARITY.getPropertyName())) {
        setUsePopularity(Boolean.parseBoolean(prop.getProperty(ConfigProperty.USE_POPULARITY.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.USE_SURFACE_FORMS.getPropertyName())) {
        setUseSurfaceForms(Boolean.parseBoolean(prop.getProperty(ConfigProperty.USE_SURFACE_FORMS.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.ALGORITHM.getPropertyName())) {
        setAlgorithm(Algorithm.valueOf(prop.getProperty(ConfigProperty.ALGORITHM.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.NER2NED_MAPPING.getPropertyName())) {
        setNER2NEDMapping(parseNER2NEDMapping(prop.getProperty(ConfigProperty.NER2NED_MAPPING.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.USE_CONTEXT.getPropertyName())) {
        setUseContext(Boolean.parseBoolean(prop.getProperty(ConfigProperty.USE_CONTEXT.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.FORCE_NER2NED_MAPPING.getPropertyName())) {
        setForceNER2NEDMapping(
            Boolean.parseBoolean(prop.getProperty(ConfigProperty.FORCE_NER2NED_MAPPING.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.USE_ACRONYM.getPropertyName())) {
        setUseAcronym(Boolean.parseBoolean(prop.getProperty(ConfigProperty.USE_ACRONYM.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.USE_COMMON_ENTITIES.getPropertyName())) {
        setUseCommonEntities(
            Boolean.parseBoolean(prop.getProperty(ConfigProperty.USE_COMMON_ENTITIES.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.CANDIDATE_CACHE_SIZE.getPropertyName())) {
        setCandidateCacheSize(
            Integer.parseInt(prop.getProperty(ConfigProperty.CANDIDATE_CACHE_SIZE.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.DISAMBIGUATION_PAGE_CACHE_SIZE.getPropertyName())) {
        setDisambiguationPageCacheSize(
            Integer.parseInt(prop.getProperty(ConfigProperty.DISAMBIGUATION_PAGE_CACHE_SIZE.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.TRIPLE_INDEX_CACHE_SIZE.getPropertyName())) {
        setTripleIndexCacheSize(
            Integer.parseInt(prop.getProperty(ConfigProperty.TRIPLE_INDEX_CACHE_SIZE.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.INDEX_TTL_PATH.getPropertyName())) {
        setIndexTTLPath(Paths.get(prop.getProperty(ConfigProperty.INDEX_TTL_PATH.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.INDEX_SURFACE_FORM_TSV_PATH.getPropertyName())) {
        setIndexSurfaceFormTSVPath(
            Paths.get(prop.getProperty(ConfigProperty.INDEX_SURFACE_FORM_TSV_PATH.getPropertyName())));
      }
      if(prop.containsKey(ConfigProperty.INLINK_FILE_PATH.getPropertyName())){
          setInLinkFilePath(
            Paths.get(prop.getProperty(ConfigProperty.INLINK_FILE_PATH.getPropertyName())));
      }
      
      if(prop.containsKey(ConfigProperty.PAGE_IDS_FILE_PATH.getPropertyName())){
          setPageIdsFilePath(
            Paths.get(prop.getProperty(ConfigProperty.PAGE_IDS_FILE_PATH.getPropertyName())));
      }
      
      if(prop.containsKey(ConfigProperty.ANCHOR_TEXT_FILE_PATH.getPropertyName())){
          setAnchorTextsFilePath(
            Paths.get(prop.getProperty(ConfigProperty.ANCHOR_TEXT_FILE_PATH.getPropertyName())));
      }
      
      if(prop.containsKey(ConfigProperty.PAGERANK_FILE_PATH.getPropertyName())){
          setPageRankFilePath(
            Paths.get(prop.getProperty(ConfigProperty.PAGERANK_FILE_PATH.getPropertyName())));
      }

    } catch (final IOException | AGDISTISConfigurationException e) {
      LOGGER.warn("Unable to load the default configuration from {}. Proceed with default values. Message {}",
          _PATH_DEFAULT_CONFIG_FILE, org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
    }
  }

  private Map<String, String> parseNER2NEDMapping(String mappingString) throws AGDISTISConfigurationException {

    final Map<String, String> res = Maps.newHashMap();
    if (mappingString.isEmpty() || (mappingString == null)) {
      return res;
    }
    final String[] entries = StringUtils.split(mappingString, ",");
    for (final String entry : entries) {
      final String[] pair = StringUtils.split(entry, ":");
      if (pair.length != 2) {
        throw new AGDISTISConfigurationException(
            "The NER 2 NED mapping definition is incorrect. Found line " + entry + " instead of key:value");
      }
      res.put(pair[0], pair[1]);
    }
    return res;
  }

  private StringDistance loadStringDistance(final String metric) throws AGDISTISConfigurationException {

    try {
      StringDistance strDistInstance;
      @SuppressWarnings("unchecked")
      final Class<? extends StringDistance> metricClass = (Class<? extends StringDistance>) Class.forName(metric);
      if (metricClass.equals(NGramDistance.class)) {
        final Constructor<?> cons = metricClass.getConstructor(int.class);
        strDistInstance = (StringDistance) cons.newInstance(CONFIGURATION.get(ConfigProperty.NGRAM_DISTANCE));
      } else {
        strDistInstance = metricClass.newInstance();
      }
      return strDistInstance;
    } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
        | IllegalAccessException | IllegalArgumentException | InvocationTargetException cnfe) {
      LOGGER.error("Unable to load string distance metric {}. StackTrace: {}", metric,
          ExceptionUtils.getStackTrace(cnfe));
      throw new AGDISTISConfigurationException(
          "Unable to load string distance metric " + metric + "StackTrace: " + ExceptionUtils.getStackTrace(cnfe));
    }
  }

  /*
   * Getters
   */
  public Path getWorkingDir() {
    return _PATH_WORKING_DIR;
  }

  public Object getPropertyValue(final ConfigProperty property) throws AGDISTISConfigurationException {
    if (CONFIGURATION.containsKey(property)) {
      return CONFIGURATION.get(property);
    }
    final String message = "Unknown property " + property.getPropertyName();
    LOGGER.error(message);
    throw new AGDISTISConfigurationException(message);
  }

  public Path getMainIndexPath() {
    return (Path) CONFIGURATION.get(ConfigProperty.MAIN_INDEX_PATH);
  }

  public Path getIndexByContextPath() {
    return (Path) CONFIGURATION.get(ConfigProperty.INDEX_BY_CONTEXT_PATH);
  }

  public URI getNodeType() {
    return (URI) CONFIGURATION.get(ConfigProperty.NODE_TYPE);
  }

  public URI getEdgeType() {
    return (URI) CONFIGURATION.get(ConfigProperty.EDGE_TYPE);
  }

  public URI getBaseURI() {
    return (URI) CONFIGURATION.get(ConfigProperty.BASE_URI);
  }

  public int getNGramDistance() {
    return (int) CONFIGURATION.get(ConfigProperty.NGRAM_DISTANCE);
  }

  public int getSemanticDepth() {
    return (int) CONFIGURATION.get(ConfigProperty.SEMANTIC_DEPTH);
  }

  public int getMaxCandidateLookups() {
    return (int) CONFIGURATION.get(ConfigProperty.MAX_CANDIDATE_LOOKUPS);
  }

  public int getMaxAcronymLookups() {
    return (int) CONFIGURATION.get(ConfigProperty.MAX_ACRONYM_LOOKUPS);
  }

  public int getMaxConnectionLookups() {
    return (int) CONFIGURATION.get(ConfigProperty.MAX_CONNECTION_LOOKUPS);
  }

  public StringDistance getCandidatePruningMetric() {
    return (StringDistance) CONFIGURATION.get(ConfigProperty.CANDIDATE_PRUNING_METRIC);
  }

  public double getCandidatePruningThreshold() {
    return (double) CONFIGURATION.get(ConfigProperty.CANDIDATE_PRUNING_THRESHOLD);
  }

  public boolean getHeuristicExpansion() {
    return (boolean) CONFIGURATION.get(ConfigProperty.HEURISTIC_EXPANSION);
  }

  public Path getPreDisambiguationWhiteListPath() {
    return (Path) CONFIGURATION.get(ConfigProperty.PRE_DISAMBIGUATION_WHITE_LIST_PATH);
  }

  public Path getPostDisambiguationWhiteListPath() {
    return (Path) CONFIGURATION.get(ConfigProperty.POST_DISAMBIGUATION_WHITE_LIST_PATH);
  }

  public Path getCorporationAffixesPath() {
    return (Path) CONFIGURATION.get(ConfigProperty.CORPORATION_AFFIXES_PATH);
  }

  public boolean getUsePopularity() {
    return (boolean) CONFIGURATION.get(ConfigProperty.USE_POPULARITY);
  }

  public boolean getUseSurfaceForms() {
    return (boolean) CONFIGURATION.get(ConfigProperty.USE_SURFACE_FORMS);
  }

  public Algorithm getAlgorithm() {
    return (Algorithm) CONFIGURATION.get(ConfigProperty.ALGORITHM);
  }

  public boolean getUseContext() {
    return (boolean) CONFIGURATION.get(ConfigProperty.USE_CONTEXT);
  }

  public boolean getForceNER2NEDMapping() {
    return (boolean) CONFIGURATION.get(ConfigProperty.FORCE_NER2NED_MAPPING);
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getNER2NEDMapping() {
    return (Map<String, String>) CONFIGURATION.get(ConfigProperty.NER2NED_MAPPING);
  }

  public boolean getUseAcronym() {
    return (boolean) CONFIGURATION.get(ConfigProperty.USE_ACRONYM);
  }

  public boolean getUseCommonEntities() {
    return (boolean) CONFIGURATION.get(ConfigProperty.USE_COMMON_ENTITIES);
  }

  public Path getIndexTTLPath() {
    return (Path) CONFIGURATION.get(ConfigProperty.INDEX_TTL_PATH);
  }

  public Path getIndexSurfaceFormTSVPath() {
    return (Path) CONFIGURATION.get(ConfigProperty.INDEX_SURFACE_FORM_TSV_PATH);
  }

  public int getCandidateCacheSize() {
    return (int) CONFIGURATION.get(ConfigProperty.CANDIDATE_CACHE_SIZE);
  }

  public int getDisambiguationPageCacheSize() {
    return (int) CONFIGURATION.get(ConfigProperty.DISAMBIGUATION_PAGE_CACHE_SIZE);
  }

  public int getTripleIndexCacheSize() {
    return (int) CONFIGURATION.get(ConfigProperty.TRIPLE_INDEX_CACHE_SIZE);
  }

  public String getAGDISTISVersion() {
    return (String) CONFIGURATION.get(ConfigProperty.AGDISTIS_VERSION);
  }

  public String getSchemaVersion() {
    return (String) CONFIGURATION.get(ConfigProperty.SCHEMA_VERSION);
  }
  
  public Path getPageIdsFilePath(){
      return (Path) CONFIGURATION.get(ConfigProperty.PAGE_IDS_FILE_PATH);
  }
  
  public Path getAnchorTextsFilePath(){
      return (Path) CONFIGURATION.get(ConfigProperty.ANCHOR_TEXT_FILE_PATH);
  }
  
  public Path getInLinkFilePath() {
      return (Path) CONFIGURATION.get(ConfigProperty.INLINK_FILE_PATH);
  }
  
  public Path getPageRankFilePath() {
      return (Path) CONFIGURATION.get(ConfigProperty.PAGERANK_FILE_PATH);
  }
  
  

  /*
   * Setters. These values override the default and file-based configuration values.
   */

  public Object setPropertyValue(final ConfigProperty property, final Object value)
      throws AGDISTISConfigurationException {
    if (CONFIGURATION.containsKey(property)) {
      CONFIGURATION.put(property, value);
    }
    final String message = "Unknown property " + property.getPropertyName();
    LOGGER.error(message);
    throw new AGDISTISConfigurationException(message);
  }

  public void setMainIndexPath(final Path mainIndexPath) {
    Preconditions.checkNotNull(mainIndexPath);
    CONFIGURATION.put(ConfigProperty.MAIN_INDEX_PATH, mainIndexPath);
  }

  public void setIndexByContextPath(final Path indexByContextPath) {
    Preconditions.checkNotNull(indexByContextPath);
    CONFIGURATION.put(ConfigProperty.INDEX_BY_CONTEXT_PATH, indexByContextPath);
  }

  public void setNodeType(final URI nodeType) {
    Preconditions.checkNotNull(nodeType);
    CONFIGURATION.put(ConfigProperty.NODE_TYPE, nodeType);
  }

  public void setEdgeType(final URI edgeType) {
    Preconditions.checkNotNull(edgeType);
    CONFIGURATION.put(ConfigProperty.EDGE_TYPE, edgeType);
  }

  public void setBaseURI(final URI baseURI) {
    Preconditions.checkNotNull(baseURI);
    CONFIGURATION.put(ConfigProperty.BASE_URI, baseURI);
  }

  public void setNGramDistance(final int ngramDistance) {
    CONFIGURATION.put(ConfigProperty.NGRAM_DISTANCE, ngramDistance);
  }

  public void setMaxCandidateLookups(final int maxCandidateLookups) {
    CONFIGURATION.put(ConfigProperty.MAX_CANDIDATE_LOOKUPS, maxCandidateLookups);
  }

  public void setMaxAcronymLookups(final int maxAcronymLookups) {
    CONFIGURATION.put(ConfigProperty.MAX_ACRONYM_LOOKUPS, maxAcronymLookups);
  }

  public void setMaxConnectionLookups(final int maxConnectionLookups) {
    CONFIGURATION.put(ConfigProperty.MAX_CONNECTION_LOOKUPS, maxConnectionLookups);
  }

  public void setSemanticDepth(final int semanticDepth) {
    CONFIGURATION.put(ConfigProperty.SEMANTIC_DEPTH, semanticDepth);
  }

  public void setCandidatePruningMetric(final String candidatePruningMetric) {
    Preconditions.checkNotNull(candidatePruningMetric);
    CONFIGURATION.put(ConfigProperty.CANDIDATE_PRUNING_METRIC, loadStringDistance((candidatePruningMetric)));
  }

  public void setCandidatePruningThreshold(final double candidatePruningThreshold) {
    CONFIGURATION.put(ConfigProperty.CANDIDATE_PRUNING_THRESHOLD, candidatePruningThreshold);
  }

  public void setHeuristicExpansion(final boolean heuristicExpansion) {
    CONFIGURATION.put(ConfigProperty.HEURISTIC_EXPANSION, heuristicExpansion);
  }

  public void setForceNER2NEDMapping(final boolean forceNER2NEDMapping) {
    CONFIGURATION.put(ConfigProperty.FORCE_NER2NED_MAPPING, forceNER2NEDMapping);
  }

  public void setPreDisambiguationWhiteListPath(final Path preDisambiguationwhiteListPath) {
    Preconditions.checkNotNull(preDisambiguationwhiteListPath);
    CONFIGURATION.put(ConfigProperty.PRE_DISAMBIGUATION_WHITE_LIST_PATH, preDisambiguationwhiteListPath);
  }

  public void setPostDisambiguationWhiteListPath(final Path postDisambiguationWhiteListPath) {
    Preconditions.checkNotNull(postDisambiguationWhiteListPath);
    CONFIGURATION.put(ConfigProperty.POST_DISAMBIGUATION_WHITE_LIST_PATH, postDisambiguationWhiteListPath);
  }

  public void setCorporationAffixesPath(final Path corporationAffixesPath) {
    Preconditions.checkNotNull(corporationAffixesPath);
    CONFIGURATION.put(ConfigProperty.CORPORATION_AFFIXES_PATH, corporationAffixesPath);
  }

  private void setNER2NEDMapping(Map<String, String> ner2ned) {
    CONFIGURATION.put(ConfigProperty.NER2NED_MAPPING, ner2ned);
  }

  public void setUsePopularity(final boolean usePopularity) {
    CONFIGURATION.put(ConfigProperty.USE_POPULARITY, usePopularity);
  }

  public void setAlgorithm(final Algorithm algorithm) {
    Preconditions.checkNotNull(algorithm);
    CONFIGURATION.put(ConfigProperty.ALGORITHM, algorithm);
  }

  public void setUseContext(final boolean useContext) {
    CONFIGURATION.put(ConfigProperty.USE_CONTEXT, useContext);
  }

  public void setUseSurfaceForms(final boolean surfaceForms) {
    CONFIGURATION.put(ConfigProperty.USE_SURFACE_FORMS, surfaceForms);
  }

  public void setUseAcronym(final boolean useAcronym) {
    CONFIGURATION.put(ConfigProperty.USE_ACRONYM, useAcronym);
  }

  public void setUseCommonEntities(final boolean useCommonEntities) {
    CONFIGURATION.put(ConfigProperty.USE_COMMON_ENTITIES, useCommonEntities);
  }

  public void setIndexTTLPath(final Path indexTTLPath) {
    Preconditions.checkNotNull(indexTTLPath);
    CONFIGURATION.put(ConfigProperty.INDEX_TTL_PATH, indexTTLPath);
  }

  public void setIndexSurfaceFormTSVPath(final Path surfaceFormTSVPath) {
    Preconditions.checkNotNull(surfaceFormTSVPath);
    CONFIGURATION.put(ConfigProperty.INDEX_SURFACE_FORM_TSV_PATH, surfaceFormTSVPath);
  }
  
  public void setPageIdsFilePath(final Path pageIdsFilePath) {
      Preconditions.checkNotNull(pageIdsFilePath);
      CONFIGURATION.put(ConfigProperty.PAGE_IDS_FILE_PATH, pageIdsFilePath);
    }
  
  public void setAnchorTextsFilePath(final Path anchorTextsFilePath) {
      Preconditions.checkNotNull(anchorTextsFilePath);
      CONFIGURATION.put(ConfigProperty.ANCHOR_TEXT_FILE_PATH, anchorTextsFilePath);
    }
  
  public void setInLinkFilePath(final Path inLinkFilePath) {
      Preconditions.checkNotNull(inLinkFilePath);
      CONFIGURATION.put(ConfigProperty.INLINK_FILE_PATH, inLinkFilePath);
    }
  
  public void setPageRankFilePath(final Path pageRankFilePath) {
      Preconditions.checkNotNull(pageRankFilePath);
      CONFIGURATION.put(ConfigProperty.PAGERANK_FILE_PATH, pageRankFilePath);
    }

  
  private void setAGDISTISVersion(final String agdistisVersion) {
    Preconditions.checkNotNull(agdistisVersion);
    CONFIGURATION.put(ConfigProperty.AGDISTIS_VERSION, agdistisVersion);
  }

  private void setSchemaVersion(final String schemaVersion) {
    Preconditions.checkNotNull(schemaVersion);
    CONFIGURATION.put(ConfigProperty.SCHEMA_VERSION, schemaVersion);
  }

  private void setCandidateCacheSize(final int size) {
    Preconditions.checkNotNull(size);
    CONFIGURATION.put(ConfigProperty.CANDIDATE_CACHE_SIZE, size);
  }

  private void setTripleIndexCacheSize(final int size) {
    Preconditions.checkNotNull(size);
    CONFIGURATION.put(ConfigProperty.TRIPLE_INDEX_CACHE_SIZE, size);
  }

  private void setDisambiguationPageCacheSize(final int size) {
    Preconditions.checkNotNull(size);
    CONFIGURATION.put(ConfigProperty.DISAMBIGUATION_PAGE_CACHE_SIZE, size);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AGDISTIS Configuration:");

    sb.append(IOUtils.LINE_SEPARATOR);
    sb.append(IOUtils.LINE_SEPARATOR);
    sb.append(ConfigProperty.MAIN_INDEX_PATH.name());
    sb.append(": ");
    sb.append(getMainIndexPath().toString());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.INDEX_BY_CONTEXT_PATH.name());
    sb.append(": ");
    sb.append(getIndexByContextPath().toString());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.NODE_TYPE.name());
    sb.append(": ");
    sb.append(getNodeType().toString());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.EDGE_TYPE.name());
    sb.append(": ");
    sb.append(getEdgeType().toString());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.BASE_URI.name());
    sb.append(": ");
    sb.append(getBaseURI().toString());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.NGRAM_DISTANCE.name());
    sb.append(": ");
    sb.append(getNGramDistance());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.SEMANTIC_DEPTH.name());
    sb.append(": ");
    sb.append(getSemanticDepth());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.CANDIDATE_PRUNING_METRIC.name());
    sb.append(": ");
    sb.append(ClassUtils.getShortCanonicalName(getCandidatePruningMetric().getClass()));
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.CANDIDATE_PRUNING_THRESHOLD.name());
    sb.append(": ");
    sb.append(getCandidatePruningThreshold());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.HEURISTIC_EXPANSION.name());
    sb.append(": ");
    sb.append(getHeuristicExpansion());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.PRE_DISAMBIGUATION_WHITE_LIST_PATH.name());
    sb.append(": ");
    sb.append(getPreDisambiguationWhiteListPath());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.POST_DISAMBIGUATION_WHITE_LIST_PATH.name());
    sb.append(": ");
    sb.append(getPostDisambiguationWhiteListPath());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.CORPORATION_AFFIXES_PATH.name());
    sb.append(": ");
    sb.append(getCorporationAffixesPath());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.USE_POPULARITY.name());
    sb.append(": ");
    sb.append(getUsePopularity());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.USE_SURFACE_FORMS.name());
    sb.append(": ");
    sb.append(getUseSurfaceForms());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.ALGORITHM.name());
    sb.append(": ");
    sb.append(getAlgorithm().name());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.USE_CONTEXT.name());
    sb.append(": ");
    sb.append(getUseContext());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.USE_ACRONYM.name());
    sb.append(": ");
    sb.append(getUseAcronym());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.FORCE_NER2NED_MAPPING.name());
    sb.append(": ");
    sb.append(getForceNER2NEDMapping());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.NER2NED_MAPPING.name());
    sb.append(": ");
    sb.append(getNER2NEDMapping());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.USE_COMMON_ENTITIES.name());
    sb.append(": ");
    sb.append(getUseCommonEntities());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.CANDIDATE_CACHE_SIZE.name());
    sb.append(": ");
    sb.append(getCandidateCacheSize());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.DISAMBIGUATION_PAGE_CACHE_SIZE.name());
    sb.append(": ");
    sb.append(getDisambiguationPageCacheSize());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.TRIPLE_INDEX_CACHE_SIZE.name());
    sb.append(": ");
    sb.append(getTripleIndexCacheSize());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.MAX_ACRONYM_LOOKUPS.name());
    sb.append(": ");
    sb.append(getMaxAcronymLookups());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.MAX_CANDIDATE_LOOKUPS.name());
    sb.append(": ");
    sb.append(getMaxCandidateLookups());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.MAX_CONNECTION_LOOKUPS.name());
    sb.append(": ");
    sb.append(getMaxConnectionLookups());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.INDEX_TTL_PATH.name());
    sb.append(": ");
    sb.append(getIndexTTLPath());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.INDEX_SURFACE_FORM_TSV_PATH.name());
    sb.append(": ");
    sb.append(getIndexSurfaceFormTSVPath());
    sb.append(IOUtils.LINE_SEPARATOR);
    
    sb.append(ConfigProperty.PAGE_IDS_FILE_PATH.name());
    sb.append(": ");
    sb.append(getPageIdsFilePath());
    sb.append(IOUtils.LINE_SEPARATOR);
    
    sb.append(ConfigProperty.ANCHOR_TEXT_FILE_PATH.name());
    sb.append(": ");
    sb.append(getAnchorTextsFilePath());
    sb.append(IOUtils.LINE_SEPARATOR);
    
    sb.append(ConfigProperty.INLINK_FILE_PATH.name());
    sb.append(": ");
    sb.append(getInLinkFilePath());
    sb.append(IOUtils.LINE_SEPARATOR);
    
    sb.append(ConfigProperty.PAGERANK_FILE_PATH.name());
    sb.append(": ");
    sb.append(getPageRankFilePath());
    sb.append(IOUtils.LINE_SEPARATOR);
    
    

    return sb.toString();
  }

  public AGDISTISConfiguration getInstance() {
    return AGDISTISConfiguration.INSTANCE;
  }





}
