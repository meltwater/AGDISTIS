package org.aksw.agdistis.util;

import java.util.List;

public class Utils {

  public static int[] convertIntegers(final List<Integer> integers) {
    final int[] ret = new int[integers.size()];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = integers.get(i).intValue();
    }
    return ret;
  }
}
