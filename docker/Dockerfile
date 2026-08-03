FROM eclipse-temurin:17-jre@sha256:92999aea37688157a53a40bfcb187c30f317422e028045fd5fc5c548fde9e626

ARG GRADLE_VERSION=7.6.4
ARG GRADLE_SHA256
ARG GRETL_VERSION
ARG JAVA_VERSION=17
ARG DUCKDB_EXTENSION_VERSION

RUN test -n "${GRETL_VERSION}" || (echo "GRETL_VERSION build argument is required" >&2; exit 1)
RUN test -n "${GRADLE_SHA256}" || (echo "GRADLE_SHA256 build argument is required" >&2; exit 1)
RUN test -n "${DUCKDB_EXTENSION_VERSION}" || (echo "DUCKDB_EXTENSION_VERSION build argument is required" >&2; exit 1)

ENV GRADLE_HOME=/opt/gradle \
    GRADLE_USER_HOME=/work/gradle-home \
    GRETL_VERSION=${GRETL_VERSION} \
    JAVA_VERSION=${JAVA_VERSION} \
    DUCKDB_EXTENSION_DIRECTORY=/opt/gretl/duckdb-extensions

LABEL org.opencontainers.image.title="GRETL" \
      org.opencontainers.image.version="${GRETL_VERSION}" \
      org.opencontainers.image.source="https://github.com/edigonzales/gretl-next"

RUN apt-get update \
    && apt-get install -y --no-install-recommends bash ca-certificates curl unzip \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && echo "${GRADLE_SHA256}  /tmp/gradle.zip" | sha256sum -c - \
    && unzip -q /tmp/gradle.zip -d /opt \
    && mv "/opt/gradle-${GRADLE_VERSION}" "${GRADLE_HOME}" \
    && ln -s "${GRADLE_HOME}/bin/gradle" /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

RUN useradd -m -u 1001 -g root gradle \
    && mkdir -p /work/project /work/gradle-home /home/gradle/project /home/gradle/.gradle \
        /opt/gretl/duckdb-extensions /opt/gretl/manifests /opt/gretl/init \
    && chown -R gradle:root /work /home/gradle /opt/gretl

COPY --chown=gradle:root gretl /usr/local/bin/gretl
COPY --chown=gradle:root init.gradle /opt/gretl/init/gretl.init.gradle
COPY --chown=gradle:root build.info /opt/gretl/build.info
COPY --chown=gradle:root maven-repo /opt/gretl/maven-repository
COPY --chown=gradle:root manifests/bundled-artifacts.json /opt/gretl/manifests/bundled-artifacts.json
COPY --chown=gradle:root manifests/dependency-closure.json /opt/gretl/manifests/dependency-closure.json
COPY --chown=gradle:root manifests/checksums.sha256 /opt/gretl/manifests/checksums.sha256

RUN chmod +x /usr/local/bin/gretl

USER gradle
RUN mkdir -p "${GRADLE_USER_HOME}" \
    && runtime_cp="$(find /opt/gretl/maven-repository -type f -name '*.jar' -printf '%p:')" \
    && java -cp "${runtime_cp}" ch.so.agi.gretl.internal.duckdb.DuckDbExtensionInstaller postgres spatial excel \
    && platform="$(uname -m)" \
    && case "${platform}" in x86_64) platform=linux_amd64 ;; aarch64) platform=linux_arm64 ;; *) echo "Unsupported DuckDB platform ${platform}" >&2; exit 1 ;; esac \
    && extension_directory="/opt/gretl/duckdb-extensions/v${DUCKDB_EXTENSION_VERSION}/${platform}" \
    && first=true \
    && { printf '{"duckDbVersion":"%s","platform":"%s","extensions":[' "${DUCKDB_EXTENSION_VERSION}" "${platform}"; \
         for spec in postgres:postgres_scanner spatial:spatial excel:excel; do \
             name="${spec%%:*}"; file="${spec#*:}.duckdb_extension"; path="${extension_directory}/${file}"; \
             test -s "${path}" || { echo "Missing DuckDB extension ${path}" >&2; exit 1; }; \
             sha="$(sha256sum "${path}" | cut -d' ' -f1)"; size="$(wc -c < "${path}" | tr -d ' ')"; \
             if [ "${first}" = false ]; then printf ','; fi; first=false; \
             printf '{"name":"%s","path":"%s","sha256":"%s","size":%s}' "${name}" "${path}" "${sha}" "${size}"; \
         done; printf ']}\n'; } > /opt/gretl/manifests/duckdb-extensions.json

USER root
RUN chmod -R a-w /opt/gretl /opt/gradle /usr/local/bin/gretl \
    && chmod 755 /work /work/project /home/gradle /home/gradle/project

USER gradle
WORKDIR /home/gradle/project

ENTRYPOINT ["gretl"]
CMD ["tasks"]
