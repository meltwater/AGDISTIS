package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import smile.data.parser.IOUtils;

public class DomainWhiteLister {
  private static Logger log = LoggerFactory.getLogger(DomainWhiteLister.class);

  private final TripleIndex index;
  HashSet<String> whiteList = new HashSet<String>();
  private final static Cache<String, Boolean> whiteListCache = CacheBuilder.newBuilder().maximumSize(50000).build();

  public DomainWhiteLister(final TripleIndex index) throws IOException {

    final Path path = AGDISTISConfiguration.INSTANCE.getWhiteListPath();
    whiteList.addAll(IOUtils.readLines(DomainWhiteLister.class.getResourceAsStream(path.toString())));
    this.index = index;
  }

  public boolean fitsIntoDomain(final String candidateURL) {

    final Boolean present = whiteListCache.getIfPresent(candidateURL);
    if (present != null) {
      log.trace("Whitelisting cache hit.");
      return present;
    }
    if (whiteList.contains(candidateURL) || whiteList.isEmpty()) {
      whiteListCache.put(candidateURL, true);
      return true;
    }
    final List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
    if (tmp.isEmpty()) {
      whiteListCache.put(candidateURL, false);
      return false;
    }
    for (final Triple triple : tmp) {
      if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory")) {
        if (whiteList.contains(triple.getObject())) {
          whiteListCache.put(candidateURL, true);
          return true;
        }
      }
    }
    whiteListCache.put(candidateURL, false);
    return false;
  }
}
