// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.node.NullNode;

/**
 * The server refused to generate a file from the data: HTTP 422 with {@code success: false}.
 *
 * <p>Since pain001 0.0.87, {@code generate} answers 422 when rows fail validation or a scheme
 * rule. The body carries the findings, not a {@code detail}, so {@link #detail()} is JSON null and
 * {@link #response()} holds the decoded body: {@link GenerateResponse#validationErrors()} and
 * {@link GenerateResponse#schemeViolations()}. Catching {@link Pain001ApiException} still catches
 * this. The message names only the status, method and path, so it is safe to log.
 */
public final class GenerationRefusedException extends Pain001ApiException {

  private static final long serialVersionUID = 1L;

  private final transient GenerateResponse response;

  /**
   * Creates the exception.
   *
   * @param response the decoded refusal body
   * @param method the HTTP method of the refused request
   * @param path the path of the refused request
   */
  public GenerationRefusedException(GenerateResponse response, String method, String path) {
    super(422, NullNode.getInstance(), method, path);
    this.response = response;
  }

  /**
   * The decoded refusal.
   *
   * @return the body: {@code success} false, no XML, and the findings
   */
  public GenerateResponse response() {
    return response;
  }
}
