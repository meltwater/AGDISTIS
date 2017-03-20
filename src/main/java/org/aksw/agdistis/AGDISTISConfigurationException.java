package org.aksw.agdistis;

/**
 * A generic exception thrown when AGDISTIS is misconfigured.
 * @author <a href="mailto:giorgio.orsi@meltwater.com">Giorgio Orsi</a>
 **/
public class AGDISTISConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AGDISTISConfigurationException(final String message) {
    super(message);
  }

  public AGDISTISConfigurationException(final String message, final Throwable throwable) {
    super(message, throwable);
  }

}
