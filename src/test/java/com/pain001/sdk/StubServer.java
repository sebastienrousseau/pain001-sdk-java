// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** A one-route HTTP server on loopback that answers every request the same way and records it. */
final class StubServer implements AutoCloseable {

  /** What the server last received. Header names are lower case. */
  record Received(String method, String path, Map<String, String> headers, String body) {}

  private final HttpServer server;
  private final int status;
  private final String body;
  private final long delayMillis;
  private volatile Received last;

  StubServer(int status, String body) throws IOException {
    this(status, body, 0);
  }

  StubServer(int status, String body, long delayMillis) throws IOException {
    this.status = status;
    this.body = body;
    this.delayMillis = delayMillis;
    this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  Received last() {
    return last;
  }

  private void handle(HttpExchange exchange) throws IOException {
    Map<String, String> headers = new TreeMap<>();
    exchange
        .getRequestHeaders()
        .forEach((k, v) -> headers.put(k.toLowerCase(Locale.ROOT), v.get(0)));
    String text = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    String path = exchange.getRequestURI().getPath();
    last = new Received(exchange.getRequestMethod(), path, headers, text);
    pause();
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  private void pause() {
    if (delayMillis > 0) {
      try {
        Thread.sleep(delayMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
