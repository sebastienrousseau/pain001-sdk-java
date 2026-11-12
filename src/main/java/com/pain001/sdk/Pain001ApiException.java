// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The server answered with a non-2xx status.
 *
 * <p>{@link #detail()} is the parsed {@code detail} member of the error body: a string for most
 * errors, an array of request-validation errors for HTTP 422. A refused {@code generate} raises the
 * subclass {@link GenerationRefusedException}, whose body has no {@code detail} but carries the
 * findings. The exception message names only the status, method and path, never request content,
 * so it is safe to log.
 */
public class Pain001ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int status;
  private final transient JsonNode detail;

  /**
   * Creates the exception.
   *
   * @param status the HTTP status code
   * @param detail the parsed {@code detail} of the error body (JSON null when absent)
   * @param method the HTTP method of the failed request
   * @param path the path of the failed request
   */
  public Pain001ApiException(int status, JsonNode detail, String method, String path) {
    super("pain001 API returned HTTP " + status + " for " + method + " " + path);
    this.status = status;
    this.detail = detail;
  }

  /** The HTTP status code. */
  public int status() {
    return status;
  }

  /** The parsed {@code detail}: a string, an array (HTTP 422) or JSON null. */
  public JsonNode detail() {
    return detail;
  }
}
