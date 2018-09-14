/*
 * MIT License
 * Copyright (c) 2018 Meltwater
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.aksw.agdistis.util;

/**
 * A strong (64-bit) string hash, used when String.hashCode() is not enough because of too many collisions.
 * @author Meltwater NLP Team (Karma)
 */
public class Hasher {
  // From http://www.javamex.com/tutorials/collections/strong_hash_code_implementation_2.shtml
  private static final long[] byteTable;
  private static final long HSTART = 0xBB40E64DA205B064L;
  private static final long HMULT = 7664345821815920749L;

  static {
    byteTable = new long[256];
    long h = 0x544B2FBACAAF1684L;
    for (int i = 0; i < 256; i++) {
      for (int j = 0; j < 31; j++) {
        h = (h >>> 7) ^ h;
        h = (h << 11) ^ h;
        h = (h >>> 10) ^ h;
      }
      byteTable[i] = h;
    }
  }

  public static long hash(String s) {
    if (s == null) {
      return 0L;
    }
    long h = HSTART;
    final long hmult = HMULT;
    final long[] ht = byteTable;
    final int len = s.length();
    for (int i = 0; i < len; i++) {
      final char ch = s.charAt(i);
      h = (h * hmult) ^ ht[ch & 0xff];
      h = (h * hmult) ^ ht[(ch >>> 8) & 0xff];
    }
    return h;
  }
}
