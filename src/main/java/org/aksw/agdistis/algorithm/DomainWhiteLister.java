package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;

import smile.data.parser.IOUtils;

public class DomainWhiteLister {
  private final TripleIndex index;
  HashSet<String> whiteList = new HashSet<String>();

  public DomainWhiteLister(final TripleIndex index) throws IOException {

    final Path path = AGDISTISConfiguration.INSTANCE.getWhiteListPath();
    whiteList.addAll(IOUtils.readLines(DomainWhiteLister.class.getResourceAsStream(path.toString())));
    this.index = index;
  }

  public boolean fitsIntoDomain(final String candidateURL) {
    final List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
    if (tmp.isEmpty()) {
      return true;
    }
    for (final Triple triple : tmp) {
      if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory")) {
        if (whiteList.contains(triple.getObject())) {
          return true;
        }
      }
    }
    return false;
  }
}
