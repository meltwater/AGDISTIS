package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import smile.data.parser.IOUtils;

public class DomainWhiteLister {
  private static Logger log = LoggerFactory.getLogger(DomainWhiteLister.class);

  private final TripleIndex index;
  private final HashSet<String> whiteList = new HashSet<String>();
  // The size of this cache should be the same as the size of the candidate cache.
  private final Cache<String, Boolean> whiteListCache = CacheBuilder.newBuilder()
      .maximumSize(AGDISTISConfiguration.INSTANCE.getCandidateCacheSize()).build();

  public DomainWhiteLister(final TripleIndex index, final Path whiteListPath) {

    try {
      whiteList.addAll(IOUtils.readLines(DomainWhiteLister.class.getResourceAsStream(whiteListPath.toString())));
    } catch (final IOException ioe) {
      log.error("Unable to load whitelist content from {}. Proceed with an empty whitelist.", whiteListPath.toString());
    }
    this.index = index;
  }

  public boolean fitsIntoDomain(final String candidateURL, Optional<String> nerType) {

    final Boolean present = whiteListCache.getIfPresent(candidateURL);
    if (present != null) {
      log.trace("Whitelisting cache hit.");
      return present;
    }
    if (whiteList.contains(candidateURL) || (whiteList.isEmpty()
        && ((!nerType.isPresent()) || (AGDISTISConfiguration.INSTANCE.getForceNER2NEDMapping() == false)))) {
      whiteListCache.put(candidateURL, true);
      return true;
    }
    final List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null, 20);
    if (tmp.isEmpty()) {
      whiteListCache.put(candidateURL, false);
      return false;
    }
    for (final Triple triple : tmp) {
      if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory")) {
        if ((whiteList.contains(triple.getObject()) || whiteList.isEmpty())
            && isNERCompliant(nerType.get(), triple.getObject())) {
          whiteListCache.put(candidateURL, true);
          return true;
        }
      }
    }
    whiteListCache.put(candidateURL, false);
    return false;
  }

  private boolean isNERCompliant(final String nerType, final String nedURI) {
    if (AGDISTISConfiguration.INSTANCE.getForceNER2NEDMapping()) {
      final String nedType = StringUtils.substringAfter(AGDISTISConfiguration.INSTANCE.getEdgeType().toString(),
          AGDISTISConfiguration.INSTANCE.getEdgeType().toString());
      if (AGDISTISConfiguration.INSTANCE.getNER2NEDMapping().containsKey(nedType)) {
        return nerType.equals(AGDISTISConfiguration.INSTANCE.getNER2NEDMapping().get(nedType));
      }
      return true;
    }
    return true;
  }
}
