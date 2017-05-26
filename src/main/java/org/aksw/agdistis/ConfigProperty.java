package org.aksw.agdistis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * AGDISTIS configuration property names.
 * @author <a href="mailto:giorgio.orsi@meltwater.com">Giorgio Orsi</a>
 **/
public enum ConfigProperty {

  //@formatter:off
  // Enum values
  MAIN_INDEX_PATH,
  INDEX_BY_CONTEXT_PATH,
  NODE_TYPE,
  EDGE_TYPE,
  BASE_URI,
  DBPEDIA_ENDPOINT,
  NGRAM_DISTANCE,
  SEMANTIC_DEPTH,
  CANDIDATE_PRUNING_METRIC,
  CANDIDATE_PRUNING_THRESHOLD,
  HEURISTIC_EXPANSION,
  PRE_DISAMBIGUATION_WHITE_LIST_PATH,
  POST_DISAMBIGUATION_WHITE_LIST_PATH,
  CORPORATION_AFFIXES_PATH,
  USE_POPULARITY,
  ALGORITHM,
  USE_CONTEXT,
  USE_SURFACE_FORMS,
  USE_ACRONYM,
  USE_COMMON_ENTITIES,
  INDEX_TTL_PATH,
  INDEX_SURFACE_FORM_TSV_PATH;
  //@formatter:on

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigProperty.class);

  private static final ImmutableMap<ConfigProperty, String> keyMap = new ImmutableMap.Builder<ConfigProperty, String>()
      .put(MAIN_INDEX_PATH, "index").put(INDEX_BY_CONTEXT_PATH, "index2").put(NODE_TYPE, "nodeType")
      .put(EDGE_TYPE, "edgeType").put(BASE_URI, "baseURI").put(DBPEDIA_ENDPOINT, "endpoint")
      .put(NGRAM_DISTANCE, "ngramDistance").put(SEMANTIC_DEPTH, "maxDepth")
      .put(CANDIDATE_PRUNING_THRESHOLD, "candidatePruningThreshold")
      .put(CANDIDATE_PRUNING_METRIC, "candidatePruningMetric").put(USE_SURFACE_FORMS, "surfaceForms")
      .put(HEURISTIC_EXPANSION, "heuristicExpansionOn")
      .put(PRE_DISAMBIGUATION_WHITE_LIST_PATH, "preDisambiguationWhiteList")
      .put(POST_DISAMBIGUATION_WHITE_LIST_PATH, "postDisambiguationWhiteList")
      .put(CORPORATION_AFFIXES_PATH, "corporationAffixes").put(USE_POPULARITY, "popularity").put(ALGORITHM, "algorithm")
      .put(USE_CONTEXT, "context").put(USE_ACRONYM, "acronym").put(USE_COMMON_ENTITIES, "commonEntities")
      .put(INDEX_TTL_PATH, "folderWithTTLFiles").put(INDEX_SURFACE_FORM_TSV_PATH, "surfaceFormTSV").build();

  public String getPropertyName() throws AGDISTISConfigurationException {
    if (keyMap.containsKey(this)) {
      return keyMap.get(this);
    }
    final String message = "Unknown property name for property " + name();
    LOGGER.error(message);
    throw new AGDISTISConfigurationException(message);
  }
}
