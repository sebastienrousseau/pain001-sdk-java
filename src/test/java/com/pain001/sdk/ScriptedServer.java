// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** A loopback server that answers requests in script order, repeating the last answer. */
final class ScriptedServer implements AutoCloseable {

  /** One scripted answer: status, extra headers and body. */
  record Answer(int status, Map<String, String> headers, String body) {
    static Answer of(int status, String body) {
      return new Answer(status, Map.of(), body);
    }
  }

  private final HttpServer server;
  private final List<Answer> script;
  private final AtomicInteger requests = new AtomicInteger();

  ScriptedServer(List<Answer> script) throws IOException {
    this.script = List.copyOf(script);
    this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  /** How many requests arrived. */
  int requests() {
    return requests.get();
  }

  private void handle(HttpExchange exchange) throws IOException {
    exchange.getRequestBody().readAllBytes();
    int index = Math.min(requests.getAndIncrement(), script.size() - 1);
    Answer answer = script.get(index);
    answer.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
    byte[] bytes = answer.body().getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(answer.status(), bytes.length == 0 ? -1 : bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
