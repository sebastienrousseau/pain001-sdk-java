// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * {@code generate} got a 2xx answer that carries no XML document.
 *
 * <p>A 2xx from {@code generate} must hold the generated XML. A server older than pain001 0.0.87
 * answers a refused file with HTTP 200 and {@code success: false}; a broken proxy or server can
 * answer 2xx with no {@code xml}. Both raise this rather than return a response without a
 * document, so a returned {@link GenerateResponse} always carries the XML. It is a broken reply,
 * not the server refusing the data, so it extends {@link UncheckedIOException} like the other
 * transport failures; it is neither a {@link Pain001ApiException} nor a {@link
 * GenerationRefusedException}. The message names only the status, method and path, never the
 * body, so it is safe to log.
 */
public final class NoDocumentException extends UncheckedIOException {

  private static final long serialVersionUID = 1L;

  /** The 2xx HTTP status code the server answered. */
  private final int status;

  /**
   * Creates the exception.
   *
   * @param status the 2xx HTTP status the server answered
   * @param method the HTTP method of the request
   * @param path the path of the request
   */
  public NoDocumentException(int status, String method, String path) {
    this(status, message(status, method, path));
  }

  private NoDocumentException(int status, String message) {
    super(message, new IOException(message));
    this.status = status;
  }

  private static String message(int status, String method, String path) {
    return "pain001 API returned HTTP " + status + " for " + method + " " + path
        + " without an XML document";
  }

  /**
   * The 2xx HTTP status code the server answered.
   *
   * @return a status from 200 to 299
   */
  public int status() {
    return status;
  }
}
