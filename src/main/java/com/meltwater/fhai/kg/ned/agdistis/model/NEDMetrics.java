/*
 * MIT License

 * Copyright (c) 2016 Meltwater

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
package com.meltwater.fhai.kg.ned.agdistis.model;

import org.apache.commons.io.IOUtils;

/**
 * This class stores general NED system performance Metrics, e.g., the time to disambiguate the document.
 * @author <a href="mailto:giorgio.orsi@meltwater.com">Giorgio Orsi</a>
 **/
public class NEDMetrics {
  private long candidateSelectionTime;
  private long disambiguationTime;

  /**
   * @return the candidateSelectionTime
   */
  public long getCandidateSelectionTime() {
    return candidateSelectionTime;
  }

  /**
   * @param candidateSelectionTime the candidateSelectionTime to set
   */
  public void setCandidateSelectionTime(final long candidateSelectionTime) {
    this.candidateSelectionTime = candidateSelectionTime;
  }

  /**
   * @return the disambiguationTime
   */
  public long getDisambiguationTime() {
    return disambiguationTime;
  }

  /**
   * @param disambiguationTime the disambiguationTime to set
   */
  public void setDisambiguationTime(final long disambiguationTime) {
    this.disambiguationTime = disambiguationTime;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + (int) (candidateSelectionTime ^ (candidateSelectionTime >>> 32));
    result = (prime * result) + (int) (disambiguationTime ^ (disambiguationTime >>> 32));
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof NEDMetrics)) {
      return false;
    }
    final NEDMetrics other = (NEDMetrics) obj;
    if (candidateSelectionTime != other.candidateSelectionTime) {
      return false;
    }
    if (disambiguationTime != other.disambiguationTime) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("NEDMetrics [");
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Candidate Selection Time:");
    builder.append(getCandidateSelectionTime());
    builder.append("msec");
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Disambiguation Time: ");
    builder.append(getDisambiguationTime());
    builder.append("msec");
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("]");
    return builder.toString();
  }

}
