<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Packaging

For integrators and anyone repackaging the SDK.

## Registry

The SDK is published to Maven Central as `com.pain001:pain001-sdk`. The
`com.pain001` namespace is verified on the Central Portal. Each release
uploads the jar, a `-sources` jar, a `-javadoc` jar and the POM, each
with a detached GPG signature (`.asc`); Maven Central adds the MD5 and
SHA-1 files.

```xml
<dependency>
  <groupId>com.pain001</groupId>
  <artifactId>pain001-sdk</artifactId>
  <version>0.0.88</version>
</dependency>
```

Gradle: `implementation("com.pain001:pain001-sdk:0.0.88")`.

The release workflow (`.github/workflows/release.yml`) builds these
from the signed tag `v0.0.88`, attaches them to the GitHub release with
a CycloneDX SBOM (`pain001-sdk-0.0.88.cdx.json`), `SHA256SUMS` and a
build provenance attestation, then publishes to Maven Central.

## Verifying a download

```sh
sha256sum -c SHA256SUMS                       # from the GitHub release
gh attestation verify pain001-sdk-0.0.88.jar --repo sebastienrousseau/pain001-sdk-java
gpg --import MAVEN-SIGNING-KEY.asc               # or fetch it from keyserver.ubuntu.com
gpg --verify pain001-sdk-0.0.88.jar.asc pain001-sdk-0.0.88.jar
```

The Maven artifacts are signed by the RSA-4096 release key
`B3F5 CCDB 5A9C 2D04 EC95  2C03 5BA4 F8F9 F0F2 F59C`
(Sebastien Rousseau, pain001-sdk release signing), committed as
`MAVEN-SIGNING-KEY.asc` and published to `keyserver.ubuntu.com` and
`keys.openpgp.org`. It expires on 2028-11-13 and will be extended or
replaced, in a signed commit, before then.

The release tag is SSH-signed; `KEYS.asc` lists the signing key and how
to verify a tag with `git tag -v`.

## Licence

Apache-2.0 OR MIT, at the user's option (`LICENSE-APACHE`,
`LICENSE-MIT`). Every file carries an SPDX identifier, checked by REUSE
in CI. The one runtime dependency, Jackson databind, is Apache-2.0.

## Toolchain and dependencies

- Builds and runs on Java 17 or newer ([toolchain policy](POLICIES.md)).
- One runtime dependency, `com.fasterxml.jackson.core:jackson-databind`,
  pinned to an exact version in `pom.xml`.
- Every artifact Maven resolves is checked against `.mvn/checksums/`.
  An offline build works from a local repository filled by one online
  `./mvnw -B verify`; `-o` then builds without the network.

## Distribution packages

N/A. This is a library consumed through Maven Central; Java libraries
are not packaged per distribution (deb, rpm, Homebrew, Nix) unless an
application in that distribution needs them, and none does. There is no
binary, man page or shell completion to install. A distribution that
wants to package it can build from the signed tag with `./mvnw -B
-DskipTests package`.
