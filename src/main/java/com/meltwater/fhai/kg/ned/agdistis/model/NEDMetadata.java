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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.aksw.agdistis.AGDISTISConfiguration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple structure to store NED metadata, such as, e.g., the model version.
 * @author <a href="mailto:giorgio.orsi@meltwater.com">Giorgio Orsi</a>
 **/
public class NEDMetadata {

  private static final Logger LOGGER = LoggerFactory.getLogger(NEDMetadata.class);

  private final Path _PROPERTIES_FILE = Paths.get("../agdistis.properties");
  private final String _SCHEMA_VERSION_PROPERTY = "schemaVersion";

  private String schemaVersion;
  private String wrapperVersion;
  private String agdistisVersion;
  private String indexVersion;
  private long enrichedOn;
  private NEDMetrics metrics;

  /**
   * @return the schemaVersion
   */
  public String getSchemaVersion() {
    return schemaVersion;
  }

  public NEDMetadata() {
    setSchemaVersion(AGDISTISConfiguration.INSTANCE.getSchemaVersion());
  }

  /**
   * @param schemaVersion the schemaVersion to set
   */
  private void setSchemaVersion(final String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  /**
   * @return the wrapperVersion
   */
  public String getWrapperVersion() {
    return wrapperVersion;
  }

  /**
   * @param wrapperVersion the wrapperVersion to set
   */
  public void setWrapperVersion(final String wrapperVersion) {
    this.wrapperVersion = wrapperVersion;
  }

  /**
   * @return the adgistisVersion
   */
  public String getAGDISTISVersion() {
    return agdistisVersion;
  }

  /**
   * @param adgistisVersion the adgistisVersion to set
   */
  public void setAGDISTISVersion(final String agdistisVersion) {
    this.agdistisVersion = agdistisVersion;
  }

  /**
   * @return the enrichedOn
   */
  public long getEnrichedOn() {
    return enrichedOn;
  }

  /**
   * @param enrichedOn the enrichedOn to set
   */
  public void setEnrichedOn(final long enrichedOn) {
    this.enrichedOn = enrichedOn;
  }

  /**
   * @return the metrics
   */
  public NEDMetrics getMetrics() {
    return metrics;
  }

  /**
   * @param metrics the metrics to set
   */
  public void setMetrics(final NEDMetrics metrics) {
    this.metrics = metrics;
  }

  /**
   * @return the indexVersion
   */
  public String getIndexVersion() {
    return indexVersion;
  }

  /**
   * @param indexVersion the indexVersion to set
   */
  public void setIndexVersion(final String indexVersion) {
    this.indexVersion = indexVersion;
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
    result = (prime * result) + ((_PROPERTIES_FILE == null) ? 0 : _PROPERTIES_FILE.hashCode());
    result = (prime * result) + ((_SCHEMA_VERSION_PROPERTY == null) ? 0 : _SCHEMA_VERSION_PROPERTY.hashCode());
    result = (prime * result) + ((agdistisVersion == null) ? 0 : agdistisVersion.hashCode());
    result = (prime * result) + (int) (enrichedOn ^ (enrichedOn >>> 32));
    result = (prime * result) + ((indexVersion == null) ? 0 : indexVersion.hashCode());
    result = (prime * result) + ((metrics == null) ? 0 : metrics.hashCode());
    result = (prime * result) + ((schemaVersion == null) ? 0 : schemaVersion.hashCode());
    result = (prime * result) + ((wrapperVersion == null) ? 0 : wrapperVersion.hashCode());
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
    if (!(obj instanceof NEDMetadata)) {
      return false;
    }
    final NEDMetadata other = (NEDMetadata) obj;
    if (_PROPERTIES_FILE == null) {
      if (other._PROPERTIES_FILE != null) {
        return false;
      }
    } else if (!_PROPERTIES_FILE.equals(other._PROPERTIES_FILE)) {
      return false;
    }
    if (_SCHEMA_VERSION_PROPERTY == null) {
      if (other._SCHEMA_VERSION_PROPERTY != null) {
        return false;
      }
    } else if (!_SCHEMA_VERSION_PROPERTY.equals(other._SCHEMA_VERSION_PROPERTY)) {
      return false;
    }
    if (agdistisVersion == null) {
      if (other.agdistisVersion != null) {
        return false;
      }
    } else if (!agdistisVersion.equals(other.agdistisVersion)) {
      return false;
    }
    if (enrichedOn != other.enrichedOn) {
      return false;
    }
    if (indexVersion == null) {
      if (other.indexVersion != null) {
        return false;
      }
    } else if (!indexVersion.equals(other.indexVersion)) {
      return false;
    }
    if (metrics == null) {
      if (other.metrics != null) {
        return false;
      }
    } else if (!metrics.equals(other.metrics)) {
      return false;
    }
    if (schemaVersion == null) {
      if (other.schemaVersion != null) {
        return false;
      }
    } else if (!schemaVersion.equals(other.schemaVersion)) {
      return false;
    }
    if (wrapperVersion == null) {
      if (other.wrapperVersion != null) {
        return false;
      }
    } else if (!wrapperVersion.equals(other.wrapperVersion)) {
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

    final Date date = new Date(getEnrichedOn());
    final DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));

    final StringBuilder builder = new StringBuilder();
    builder.append("NEDMetadata [");
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Schema Version: ");
    builder.append(getSchemaVersion());
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Wrapper Version: ");
    builder.append(getWrapperVersion());
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Adgistis Version: ");
    builder.append(getAGDISTISVersion());
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("Index Version: ");
    builder.append(getIndexVersion());
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("enriched On: ");
    builder.append(format.format(date));
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append(metrics);
    builder.append(IOUtils.LINE_SEPARATOR);
    builder.append("]");
    return builder.toString();
  }
}
