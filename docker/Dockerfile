FROM eclipse-temurin:17-jre

ARG GRADLE_VERSION=7.6.4

ENV GRADLE_HOME=/opt/gradle \
    GRADLE_USER_HOME=/home/gradle/.gradle

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
COPY --chown=gradle:root maven-repo /home/gradle/maven-repo
COPY --chown=gradle:root libs /home/gradle/libs

RUN chmod +x /usr/local/bin/gretl

WORKDIR /home/gradle/project
USER gradle

ENTRYPOINT ["gretl"]
CMD ["tasks"]
