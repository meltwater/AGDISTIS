package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import smile.data.parser.IOUtils;

public class DomainWhiteLister {
  private final TripleIndex index;
  HashSet<String> whiteList = new HashSet<String>();
  private final Cache<String, Boolean> whiteListCache = CacheBuilder.newBuilder().maximumSize(50000).build();

  public DomainWhiteLister(final TripleIndex index) throws IOException {

    final Path path = AGDISTISConfiguration.INSTANCE.getWhiteListPath();
    whiteList.addAll(IOUtils.readLines(DomainWhiteLister.class.getResourceAsStream(path.toString())));
    this.index = index;
  }

  public boolean fitsIntoDomain(final String candidateURL) {

    final Boolean present = whiteListCache.getIfPresent(candidateURL);
    if (present != null) {
      return present;
    }
    final List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
    if (tmp.isEmpty()) {
      whiteListCache.put(candidateURL, true);
      return true;
    }
    for (final Triple triple : tmp) {
      if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory")) {
        if (whiteList.contains(triple.getObject())) {
          whiteListCache.put(candidateURL, true);
          return true;
        }
      }
    }
    return false;
  }
}
