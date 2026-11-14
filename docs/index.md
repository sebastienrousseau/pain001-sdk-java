<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# pain001-sdk-java

A typed Java client for the pain001 REST API, which generates and
validates ISO 20022 pain.001 and pain.008 payment files.

The SDK sends requests to `pain001 serve` and maps the answers to Java
records. The server does the payment work; the shared conformance kit,
run against a live server in CI, proves that this client and the four
sibling SDKs agree with it.

```xml
<dependency>
  <groupId>com.pain001</groupId>
  <artifactId>pain001-sdk</artifactId>
  <version>0.0.88</version>
</dependency>
```

```java
Pain001Client client = Pain001Client.builder()
    .baseUrl("http://127.0.0.1:8000")
    .build();
GenerateResponse result = client.generate(csv, "payments.csv", "pain.001.001.09");
```

## Where to go next

- [Overview](readme.md): install, quick start, every method and option.
- [API reference](https://sebastienrousseau.github.io/pain001-sdk-java/api/):
  the Javadoc for `com.pain001.sdk`.
- [Architecture](ARCHITECTURE.md): how a call flows and where each
  guarantee is enforced.
- [Benchmarks](BENCHMARKS.md): the client's own cost and loopback round
  trips, per method.
- [Development](development.md): every CI gate, reproduced locally.
