FROM eclipse-temurin:17-jre

ARG GRADLE_VERSION=7.6.4
ARG GRETL_VERSION
ARG JAVA_VERSION=17

RUN test -n "${GRETL_VERSION}" || (echo "GRETL_VERSION build argument is required" >&2; exit 1)

ENV GRADLE_HOME=/opt/gradle \
    GRADLE_USER_HOME=/home/gradle/.gradle \
    GRETL_VERSION=${GRETL_VERSION} \
    JAVA_VERSION=${JAVA_VERSION}

LABEL org.opencontainers.image.title="GRETL" \
      org.opencontainers.image.version="${GRETL_VERSION}" \
      org.opencontainers.image.source="https://github.com/edigonzales/gretl-next"

RUN apt-get update \
    && apt-get install -y --no-install-recommends bash ca-certificates curl unzip \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -q /tmp/gradle.zip -d /opt \
    && mv "/opt/gradle-${GRADLE_VERSION}" "${GRADLE_HOME}" \
    && ln -s "${GRADLE_HOME}/bin/gradle" /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

RUN useradd -m -u 1001 -g root gradle \
    && mkdir -p /home/gradle/project /home/gradle/.gradle \
    && chgrp -R root /home/gradle \
    && chmod -R g+rwX /home/gradle

COPY --chown=gradle:root gretl /usr/local/bin/gretl
COPY --chown=gradle:root init.gradle /home/gradle/init.gradle
COPY --chown=gradle:root build.info /home/gradle/build.info
COPY --chown=gradle:root maven-repo /opt/gretl/maven-repository

RUN chmod +x /usr/local/bin/gretl

USER gradle
RUN mkdir -p "${GRADLE_USER_HOME}" \
    && runtime_cp="$(find /opt/gretl/maven-repository -type f -name '*.jar' -printf '%p:')" \
    && java -cp "${runtime_cp}" ch.so.agi.gretl.internal.duckdb.DuckDbExtensionInstaller postgres spatial excel

WORKDIR /home/gradle/project

ENTRYPOINT ["gretl"]
CMD ["tasks"]
