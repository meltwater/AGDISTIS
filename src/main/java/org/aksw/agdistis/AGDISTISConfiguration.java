package org.aksw.agdistis;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.lucene.search.spell.NGramDistance;
import org.apache.lucene.search.spell.StringDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import net.logstash.logback.encoder.org.apache.commons.lang.exception.ExceptionUtils;

/**
 * A (singleton) configuration class for AGDISTIS.
 * @author <a href="mailto:giorgio.orsi@meltwater.com">Giorgio Orsi</a>
 **/
public class AGDISTISConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AGDISTISConfiguration.class);
  private static final Path _PATH_WORKING_DIR = Paths.get(System.getProperty("user.dir"));
  private static final Path _PATH_DEFAULT_CONFIG_FILE = Paths.get("/config/agdistis.properties");

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
    setDBPediaEndpoint(URI.create("http://live.dbpedia.org/sparql"));
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
    setUseCommonEntities(true);
    setIndexTTLPath(Paths.get("data/en"));
    setIndexSurfaceFormTSVPath(Paths.get("data/en/surface/en_surface_forms.tsv"));

    // Attempt to load the configuration file. Values in the file-based configuration override the default
    // configuration.
    try {
      final Properties prop = new Properties();
      final InputStream input = AGDISTISConfiguration.class.getResourceAsStream(_PATH_DEFAULT_CONFIG_FILE.toString());
      prop.load(input);

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
      if (prop.containsKey(ConfigProperty.DBPEDIA_ENDPOINT.getPropertyName())) {
        setDBPediaEndpoint(URI.create(prop.getProperty(ConfigProperty.DBPEDIA_ENDPOINT.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.NGRAM_DISTANCE.getPropertyName())) {
        setNGramDistance(Integer.parseInt(prop.getProperty(ConfigProperty.NGRAM_DISTANCE.getPropertyName())));
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
      if (prop.containsKey(ConfigProperty.ALGORITHM.getPropertyName())) {
        setAlgorithm(Algorithm.valueOf(prop.getProperty(ConfigProperty.ALGORITHM.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.USE_CONTEXT.getPropertyName())) {
        setUseContext(Boolean.parseBoolean(prop.getProperty(ConfigProperty.USE_CONTEXT.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.USE_ACRONYM.getPropertyName())) {
        setUseAcronym(Boolean.parseBoolean(prop.getProperty(ConfigProperty.USE_ACRONYM.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.USE_COMMON_ENTITIES.getPropertyName())) {
        setUseCommonEntities(
            Boolean.parseBoolean(prop.getProperty(ConfigProperty.USE_COMMON_ENTITIES.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.INDEX_TTL_PATH.getPropertyName())) {
        setIndexTTLPath(Paths.get(prop.getProperty(ConfigProperty.INDEX_TTL_PATH.getPropertyName())));
      }
      if (prop.containsKey(ConfigProperty.INDEX_SURFACE_FORM_TSV_PATH.getPropertyName())) {
        setIndexSurfaceFormTSVPath(
            Paths.get(prop.getProperty(ConfigProperty.INDEX_SURFACE_FORM_TSV_PATH.getPropertyName())));
      }
    } catch (final IOException ioe) {
      LOGGER.warn("Unable to load the default configuration from {}. Proceed with default values",
          _PATH_DEFAULT_CONFIG_FILE);
    }
  }

  private StringDistance loadStringDistance(final String metric) throws AGDISTISConfigurationException {

    try {

      StringDistance strDistInstance;
      @SuppressWarnings("unchecked")
      final Class<StringDistance> metricClass = (Class<StringDistance>) Class.forName(metric);
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
          ExceptionUtils.getFullStackTrace(cnfe));
      throw new AGDISTISConfigurationException(
          "Unable to load string distance metric " + metric + "StackTrace: " + ExceptionUtils.getFullStackTrace(cnfe));
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

  public URI getDBPediaEndpoint() {
    return (URI) CONFIGURATION.get(ConfigProperty.DBPEDIA_ENDPOINT);
  }

  public int getNGramDistance() {
    return (int) CONFIGURATION.get(ConfigProperty.NGRAM_DISTANCE);
  }

  public int getSemanticDepth() {
    return (int) CONFIGURATION.get(ConfigProperty.SEMANTIC_DEPTH);
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

  public void setDBPediaEndpoint(final URI dbpediaEndpoint) {
    Preconditions.checkNotNull(dbpediaEndpoint);
    CONFIGURATION.put(ConfigProperty.DBPEDIA_ENDPOINT, dbpediaEndpoint);
  }

  public void setNGramDistance(final int ngramDistance) {
    CONFIGURATION.put(ConfigProperty.NGRAM_DISTANCE, ngramDistance);
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

    sb.append(ConfigProperty.DBPEDIA_ENDPOINT.name());
    sb.append(": ");
    sb.append(getDBPediaEndpoint().toString());
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

    sb.append(ConfigProperty.USE_COMMON_ENTITIES.name());
    sb.append(": ");
    sb.append(getUseCommonEntities());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.INDEX_TTL_PATH.name());
    sb.append(": ");
    sb.append(getIndexTTLPath());
    sb.append(IOUtils.LINE_SEPARATOR);

    sb.append(ConfigProperty.INDEX_SURFACE_FORM_TSV_PATH.name());
    sb.append(": ");
    sb.append(getIndexSurfaceFormTSVPath());
    sb.append(IOUtils.LINE_SEPARATOR);

    return sb.toString();
  }

  public AGDISTISConfiguration getInstance() {
    return AGDISTISConfiguration.INSTANCE;
  }

}
