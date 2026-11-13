// SPDX-License-Identifier: Apache-2.0 OR MIT

/**
 * A typed Java client for the pain001 REST API ({@code pain001 serve}).
 *
 * <p>{@link com.pain001.sdk.Pain001Client} sends the requests; the records in this package carry
 * the responses. A non-2xx response raises {@link com.pain001.sdk.Pain001ApiException}, or its
 * subclass {@link com.pain001.sdk.GenerationRefusedException} with the findings when the server
 * refuses to generate a file. The
 * client never logs request or response bodies, because they hold payment data.
 */
package com.pain001.sdk;
