// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The raw result of {@link Pain001Client#request}: the HTTP status and the parsed body.
 *
 * @param status the HTTP status code
 * @param body the parsed body; JSON null when empty, a JSON string when it was not JSON
 */
public record ApiResponse(int status, JsonNode body) {

  /**
   * Whether the status is 2xx.
   *
   * @return true for 200 to 299
   */
  public boolean isSuccess() {
    return status >= 200 && status < 300;
  }
}
