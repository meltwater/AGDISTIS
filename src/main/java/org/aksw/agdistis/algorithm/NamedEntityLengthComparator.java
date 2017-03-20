package org.aksw.agdistis.algorithm;

import java.util.Comparator;

import org.aksw.agdistis.datatypes.NamedEntityInText;

/**
 * Comparator for sorting Named Entities according to their length
 * @author r.usbeck
 */
public class NamedEntityLengthComparator implements Comparator<NamedEntityInText> {

  @Override
  public int compare(final NamedEntityInText o1, final NamedEntityInText o2) {
    return o2.getLength() - o1.getLength();
  }

}
