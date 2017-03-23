/*
 * Copyright (C) 2016 diegomoussallem
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.agdistis.util;

import org.apache.commons.lang.StringUtils;

/**
 * @author diegomoussallem
 */
public class PreprocessingNLP {

  private final static String[] _SPECIAL_CHARS = new String[] { "#", ",", "_", "-", "." };
  private final static String[] _SPECIAL_CHARS_REPLACEMENTS = new String[] { "", "", " ", " ", "" };

  public String preprocess(String label) {

    final Word2num w2n = new Word2num(); // test conversion of numbers
    // Label treatment
    // System.out.println("before preprocessing: " + label);
    final String result = w2n.replaceNumbers(label);
    if (!result.equals("000")) {
      label = result;
    }
    label = StringUtils.normalizeSpace(label);
    label = StringUtils.replaceEachRepeatedly(label, _SPECIAL_CHARS, _SPECIAL_CHARS_REPLACEMENTS);
    label = StringUtils.remove(label, (char) 8203);
    if (label.equals(label.toUpperCase()) && (label.length() > 4)) {
      label = label.substring(0, 1).toUpperCase() + label.substring(1).toLowerCase();

    }

    if (label.equals(label.toLowerCase()) && (label.length() > 4)) {
      label = label.substring(0, 1).toUpperCase() + label.substring(1);
    }

    return label;
  }
}
