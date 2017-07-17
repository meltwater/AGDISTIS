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

import java.net.URL;

/**
 * TODO type comment.
 * @author <a href="mailto:giorgio.orsi@meltwater.com">Giorgio Orsi</a>
 * @version 0.1
 **/
public class DocumentData {
  private String documentId;
  private URL sourceURL;

  /**
   * @return the documentId
   */
  public String getDocumentId() {
    return documentId;
  }

  /**
   * @param documentId the documentId to set
   */
  public void setDocumentId(final String documentId) {
    this.documentId = documentId;
  }

  /**
   * @return the sourceURL
   */
  public URL getSourceURL() {
    return sourceURL;
  }

  /**
   * @param sourceURL the sourceURL to set
   */
  public void setSourceURL(final URL sourceURL) {
    this.sourceURL = sourceURL;
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
    result = (prime * result) + ((documentId == null) ? 0 : documentId.hashCode());
    result = (prime * result) + ((sourceURL == null) ? 0 : sourceURL.hashCode());
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
    if (!(obj instanceof DocumentData)) {
      return false;
    }
    final DocumentData other = (DocumentData) obj;
    if (documentId == null) {
      if (other.documentId != null) {
        return false;
      }
    } else if (!documentId.equals(other.documentId)) {
      return false;
    }
    if (sourceURL == null) {
      if (other.sourceURL != null) {
        return false;
      }
    } else if (!sourceURL.equals(other.sourceURL)) {
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
    builder.append("DocumentData [");
    builder.append("Document Id: ");
    builder.append(documentId);
    builder.append("Source URL: ");
    builder.append(sourceURL);
    builder.append("]");
    return builder.toString();
  }
}
