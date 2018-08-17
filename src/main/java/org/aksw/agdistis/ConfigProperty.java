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
  NGRAM_DISTANCE,
  MAX_CANDIDATE_LOOKUPS,
  MAX_ACRONYM_LOOKUPS,
  MAX_CONNECTION_LOOKUPS,
  SEMANTIC_DEPTH,
  CANDIDATE_PRUNING_METRIC,
  CANDIDATE_PRUNING_THRESHOLD,
  HEURISTIC_EXPANSION,
  PRE_DISAMBIGUATION_WHITE_LIST_PATH,
  POST_DISAMBIGUATION_WHITE_LIST_PATH,
  CORPORATION_AFFIXES_PATH,
  USE_POPULARITY,
  RESOLVE_OVERLAPS,
  ALGORITHM,
  USE_CONTEXT,
  SCHEMA_VERSION,
  USE_SURFACE_FORMS,
  USE_ACRONYM,
  USE_COMMON_ENTITIES,
  INDEX_TTL_PATH,
  INDEX_SURFACE_FORM_TSV_PATH,
  AGDISTIS_VERSION,
  FORCE_NER2NED_MAPPING,
  NER2NED_MAPPING,
  CANDIDATE_CACHE_SIZE,
  DISAMBIGUATION_PAGE_CACHE_SIZE,
  TRIPLE_INDEX_CACHE_SIZE, 
  PAGE_IDS_FILE_PATH, ANCHOR_TEXT_FILE_PATH, INLINK_FILE_PATH;
  //@formatter:on

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigProperty.class);

  private static final ImmutableMap<ConfigProperty, String> propKeyMap = new ImmutableMap.Builder<ConfigProperty, String>()
      .put(MAIN_INDEX_PATH, "index").put(INDEX_BY_CONTEXT_PATH, "index2").put(NODE_TYPE, "nodeType")
      .put(EDGE_TYPE, "edgeType").put(BASE_URI, "baseURI").put(NGRAM_DISTANCE, "ngramDistance")
      .put(SEMANTIC_DEPTH, "maxDepth").put(CANDIDATE_PRUNING_THRESHOLD, "candidatePruningThreshold")
      .put(CANDIDATE_PRUNING_METRIC, "candidatePruningMetric").put(USE_SURFACE_FORMS, "surfaceForms")
      .put(HEURISTIC_EXPANSION, "heuristicExpansionOn")
      .put(PRE_DISAMBIGUATION_WHITE_LIST_PATH, "preDisambiguationWhiteList")
      .put(POST_DISAMBIGUATION_WHITE_LIST_PATH, "postDisambiguationWhiteList")
      .put(CORPORATION_AFFIXES_PATH, "corporationAffixes").put(USE_POPULARITY, "popularity").put(ALGORITHM, "algorithm")
      .put(USE_CONTEXT, "context").put(USE_ACRONYM, "acronym").put(USE_COMMON_ENTITIES, "commonEntities")
      .put(INDEX_TTL_PATH, "folderWithTTLFiles").put(MAX_CANDIDATE_LOOKUPS, "maxCandidateLookups")
      .put(MAX_ACRONYM_LOOKUPS, "maxAcronymLookups").put(MAX_CONNECTION_LOOKUPS, "maxConnectionLookups")
      .put(INDEX_SURFACE_FORM_TSV_PATH, "surfaceFormTSV").put(AGDISTIS_VERSION, "agdistisVersion")
      .put(RESOLVE_OVERLAPS, "resolveOverlaps").put(SCHEMA_VERSION, "schemaVersion")
      .put(FORCE_NER2NED_MAPPING, "forceNER2NEDMapping").put(NER2NED_MAPPING, "NER2NEDMapping")
      .put(CANDIDATE_CACHE_SIZE, "candidateCacheSize")
      .put(DISAMBIGUATION_PAGE_CACHE_SIZE, "disambiguationPageCacheSize")
      .put(TRIPLE_INDEX_CACHE_SIZE, "tripleIndexCacheSize")
      .put(PAGE_IDS_FILE_PATH,"pageIdsFilePath")
      .put(ANCHOR_TEXT_FILE_PATH,"anchorTextFilePath")
      .put(INLINK_FILE_PATH,"inLinksFilePath").build();
      
      

  public String getPropertyName() throws AGDISTISConfigurationException {
    if (propKeyMap.containsKey(this)) {
      return propKeyMap.get(this);
    }
    final String message = "Unknown property name for property " + name();
    LOGGER.error(message);
    throw new AGDISTISConfigurationException(message);
  }
}
