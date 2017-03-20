package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.apache.commons.io.IOUtils;

public class CorporationAffixCleaner {

  HashSet<String> corporationAffixes = new HashSet<String>();

  public CorporationAffixCleaner() throws IOException {
    final Path path = AGDISTISConfiguration.INSTANCE.getCorporationAffixesPath();
    corporationAffixes.addAll(IOUtils.readLines(CorporationAffixCleaner.class.getResourceAsStream(path.toString())));
  }

  String cleanLabelsfromCorporationIdentifier(String label) {
    for (final String corporationAffix : corporationAffixes) {
      if (label.endsWith(corporationAffix)) {
        label = label.substring(0, label.lastIndexOf(corporationAffix));
      }
    }
    return label.trim();
  }

}
