// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A client for the pain001 REST API ({@code pain001 serve}).
 *
 * <pre>{@code
 * Pain001Client client = Pain001Client.builder()
 *     .baseUrl("http://127.0.0.1:8000")
 *     .timeout(Duration.ofSeconds(10))
 *     .build();
 * GenerateResponse result = client.generate(csv, "payments.csv", "pain.001.001.09");
 * }</pre>
 *
 * <p>Typed methods raise {@link Pain001ApiException} on a non-2xx response ({@link
 * GenerationRefusedException}, with the findings, when {@code generate} is refused) and {@link
 * UncheckedIOException} when the request cannot be sent or times out. The client does not retry,
 * and never logs request or response bodies. Instances are immutable and thread-safe.
 */
public final class Pain001Client {

  private static final Pattern FACE_NAME = Pattern.compile("[A-Za-z0-9_-]+");
  private static final String JSON = "application/json";

  private final String baseUrl;
  private final String apiKey;
  private final Duration timeout;
  private final HttpClient http;

  private Pain001Client(Builder builder) {
    this.baseUrl = builder.baseUrl;
    this.apiKey = builder.apiKey;
    this.timeout = builder.timeout;
    this.http =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(builder.timeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  /** Starts a builder; {@link Builder#baseUrl} is required. */
  public static Builder builder() {
    return new Builder();
  }

  /** {@code GET /api/v1/health}. */
  public HealthResponse health() {
    return HealthResponse.from(call("GET", "/api/v1/health", null));
  }

  /** {@code POST /api/v1/ui/validate} with no scheme. */
  public ValidationResponse validate(String content, String filename, String messageType) {
    return validate(content, filename, messageType, null);
  }

  /**
   * {@code POST /api/v1/ui/validate}: validates an uploaded payment file.
   *
   * @param content the file's text (CSV, JSON or JSONL)
   * @param filename a name whose suffix picks the parser, for example {@code payments.csv}
   * @param messageType the ISO 20022 message type, for example {@code pain.001.001.09}
   * @param scheme comma-separated scheme rulebooks to enforce, or null
   */
  public ValidationResponse validate(
      String content, String filename, String messageType, String scheme) {
    ObjectNode request = filePayload(content, filename, messageType, scheme);
    return ValidationResponse.from(call("POST", "/api/v1/ui/validate", request));
  }

  /** {@code POST /api/v1/ui/generate} with no scheme. */
  public GenerateResponse generate(String content, String filename, String messageType) {
    return generate(content, filename, messageType, null);
  }

  /**
   * {@code POST /api/v1/ui/generate}: generates the XML for an uploaded payment file.
   *
   * @param content the file's text (CSV, JSON or JSONL)
   * @param filename a name whose suffix picks the parser, for example {@code payments.csv}
   * @param messageType the ISO 20022 message type, for example {@code pain.001.001.09}
   * @param scheme comma-separated scheme rulebooks to enforce, or null
   * @return the generated file; {@link GenerateResponse#success()} is true
   * @throws GenerationRefusedException when the server refuses the data (HTTP 422 with {@code
   *     success: false}); its {@link GenerationRefusedException#response()} holds the findings
   */
  public GenerateResponse generate(
      String content, String filename, String messageType, String scheme) {
    ObjectNode request = filePayload(content, filename, messageType, scheme);
    String path = "/api/v1/ui/generate";
    ApiResponse response = request("POST", path, request);
    if (isRefusal(response)) {
      throw new GenerationRefusedException(GenerateResponse.from(response.body()), "POST", path);
    }
    return GenerateResponse.from(checked(response, "POST", path));
  }

  /** {@code POST /api/v1/check} with the default rungs. */
  public CheckResponse check(String xml) {
    return check(xml, null, null);
  }

  /**
   * {@code POST /api/v1/check}: checks a pain.001 or pain.008 document.
   *
   * @param xml the document as XML text
   * @param scheme comma-separated scheme profiles to add, or null
   * @param schematron a Schematron preset ({@code sepa}, {@code fednow}, {@code cbpr}), or null
   */
  public CheckResponse check(String xml, String scheme, String schematron) {
    ObjectNode request = Json.object();
    request.put("xml", xml);
    putIfPresent(request, "scheme", scheme);
    putIfPresent(request, "schematron", schematron);
    return CheckResponse.from(call("POST", "/api/v1/check", request));
  }

  /** {@code GET /api/v1/faces}: the faces the server can project to. */
  public List<FaceInfo> faces() {
    JsonNode body = call("GET", "/api/v1/faces", null);
    return Json.elements(body).stream().map(FaceInfo::from).toList();
  }

  /** {@code POST /api/v1/faces/{face}} with the edition detected and no bindings. */
  public FaceResponse projectFace(String face, String xml) {
    return projectFace(face, xml, null, null);
  }

  /**
   * {@code POST /api/v1/faces/{face}}: projects a pain.001 document onto a target standard.
   *
   * @param face a face name from {@link #faces()}, for example {@code berlin_group}
   * @param xml the pain.001 document as XML text
   * @param version the pain.001 edition, or null to detect it from the namespace
   * @param bindings side inputs the message does not carry (values may be maps, lists, strings or
   *     numbers), or null
   * @throws IllegalArgumentException when {@code face} is not a plain name
   */
  public FaceResponse projectFace(
      String face, String xml, String version, Map<String, ?> bindings) {
    if (face == null || !FACE_NAME.matcher(face).matches()) {
      throw new IllegalArgumentException("face must be a plain name such as berlin_group");
    }
    ObjectNode request = Json.object();
    request.put("xml", xml);
    putIfPresent(request, "version", version);
    if (bindings != null) {
      request.set("bindings", Json.toNode(bindings));
    }
    return FaceResponse.from(call("POST", "/api/v1/faces/" + face, request));
  }

  /**
   * Sends one request and returns the status and parsed body, whatever the status.
   *
   * <p>This is the low-level call the conformance runner uses; it does not raise on a non-2xx
   * status.
   *
   * @param method the HTTP method, for example {@code GET}
   * @param path the path below the base URL, starting with {@code /}
   * @param body the JSON body, or null to send none
   * @throws UncheckedIOException when the request cannot be sent or times out
   */
  public ApiResponse request(String method, String path, JsonNode body) {
    HttpRequest httpRequest = buildRequest(method, path, body);
    try {
      HttpResponse<String> response =
          http.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return new ApiResponse(response.statusCode(), Json.parseLenient(response.body()));
    } catch (IOException e) {
      throw new UncheckedIOException(failure(method, path), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      InterruptedIOException cause = new InterruptedIOException("interrupted");
      throw new UncheckedIOException(failure(method, path), cause);
    }
  }

  private HttpRequest buildRequest(String method, String path, JsonNode body) {
    Objects.requireNonNull(method, "method");
    if (path == null || !path.startsWith("/")) {
      throw new IllegalArgumentException("path must start with /");
    }
    HttpRequest.BodyPublisher publisher =
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8);
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(timeout)
            .header("Accept", JSON)
            .method(method.toUpperCase(Locale.ROOT), publisher);
    if (body != null) {
      builder.header("Content-Type", JSON);
    }
    if (apiKey != null) {
      builder.header("Authorization", "Bearer " + apiKey);
    }
    return builder.build();
  }

  private JsonNode call(String method, String path, JsonNode body) {
    return checked(request(method, path, body), method, path);
  }

  private static JsonNode checked(ApiResponse response, String method, String path) {
    if (!response.isSuccess()) {
      throw new Pain001ApiException(response.status(), detail(response.body()), method, path);
    }
    return response.body();
  }

  /** HTTP 422 whose body is a generate result with {@code success: false}, not a {@code detail}. */
  private static boolean isRefusal(ApiResponse response) {
    JsonNode success = response.body().path("success");
    return response.status() == 422 && success.isBoolean() && !success.asBoolean();
  }

  private static JsonNode detail(JsonNode body) {
    if (body.isObject()) {
      return body.has("detail") ? body.get("detail") : NullNode.getInstance();
    }
    return body;
  }

  private static ObjectNode filePayload(
      String content, String filename, String messageType, String scheme) {
    ObjectNode payload = Json.object();
    payload.put("filename", filename);
    payload.put("content", content);
    payload.put("message_type", messageType);
    putIfPresent(payload, "scheme", scheme);
    return payload;
  }

  private static void putIfPresent(ObjectNode node, String field, String value) {
    if (value != null) {
      node.put(field, value);
    }
  }

  private static String failure(String method, String path) {
    return "pain001 API request " + method + " " + path + " failed";
  }

  /** Builds a {@link Pain001Client}. */
  public static final class Builder {

    private String baseUrl;
    private String apiKey;
    private Duration timeout = Duration.ofSeconds(30);

    private Builder() {}

    /**
     * The server's base URL, for example {@code http://127.0.0.1:8000}: http or https, with a
     * host, and no query or fragment. A trailing slash is ignored.
     */
    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    /** An API key, sent as {@code Authorization: Bearer <key>}; null sends none. */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /** The connect and per-request timeout; 30 seconds when not set. */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Builds the client.
     *
     * @throws IllegalArgumentException when the base URL is missing or malformed, or the timeout
     *     is not positive
     */
    public Pain001Client build() {
      this.baseUrl = normalise(baseUrl);
      if (timeout == null || timeout.isZero() || timeout.isNegative()) {
        throw new IllegalArgumentException("timeout must be positive");
      }
      return new Pain001Client(this);
    }

    private static String normalise(String value) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("baseUrl is required");
      }
      URI uri = URI.create(value.strip());
      if (!isPlainWebUrl(uri)) {
        throw new IllegalArgumentException("baseUrl must be an http(s) URL with a host");
      }
      String text = uri.toString();
      return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
    }

    private static boolean isPlainWebUrl(URI uri) {
      String scheme = String.valueOf(uri.getScheme()).toLowerCase(Locale.ROOT);
      boolean web = "http".equals(scheme) || "https".equals(scheme);
      boolean bare = uri.getRawQuery() == null && uri.getRawFragment() == null;
      return web && bare && uri.getHost() != null;
    }
  }
}
