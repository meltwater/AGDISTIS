package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import net.logstash.logback.encoder.org.apache.commons.lang.ArrayUtils;

public class CorporationAffixCleaner {

  private final HashSet<String> corporationAffixes = new HashSet<String>();
  private final String _TOKEN_DELIMITERS = " ";

  public CorporationAffixCleaner() throws IOException {
    final Path path = AGDISTISConfiguration.INSTANCE.getCorporationAffixesPath();
    corporationAffixes.addAll(IOUtils.readLines(CorporationAffixCleaner.class.getResourceAsStream(path.toString())));
  }

  public String cleanLabelsfromCorporationIdentifier(String label) {
    label = StringUtils.remove(label, ',');
    final String[] tokens = StringUtils.split(label, _TOKEN_DELIMITERS);
    if ((tokens.length > 0) && corporationAffixes.contains(tokens[tokens.length - 1])) {
      label = StringUtils.join(ArrayUtils.remove(tokens, tokens.length - 1), " ");
    }
    return label.trim();
  }

}
