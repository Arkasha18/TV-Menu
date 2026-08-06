# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk-jammy@sha256:723151f3fc88ca2060153ee08ab8dbbea7983d6ed6f2622fe440acf178737c94

ARG DEBIAN_FRONTEND=noninteractive
ARG ANDROID_COMMAND_LINE_TOOLS_VERSION=13114758
ARG ANDROID_COMMAND_LINE_TOOLS_SHA256=7ec965280a073311c339e571cd5de778b9975026cfcbe79f2b1cdcb1e15317ee
ARG OCI_REVISION=unknown

LABEL org.opencontainers.image.title="TV Menu build environment" \
      org.opencontainers.image.description="JDK 17 and Android SDK 35 for reproducible TV Menu builds" \
      org.opencontainers.image.source="https://github.com/Arkasha18/TV-Menu" \
      org.opencontainers.image.revision="${OCI_REVISION}" \
      org.opencontainers.image.licenses="PolyForm-Noncommercial-1.0.0"

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    PATH=/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:${PATH}

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        ca-certificates \
        curl \
        git \
        unzip \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" /tmp/android-command-line-tools \
    && curl --fail --location --retry 3 \
        "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_COMMAND_LINE_TOOLS_VERSION}_latest.zip" \
        --output /tmp/android-command-line-tools.zip \
    && echo "${ANDROID_COMMAND_LINE_TOOLS_SHA256}  /tmp/android-command-line-tools.zip" \
        | sha256sum --check --strict \
    && unzip -q /tmp/android-command-line-tools.zip -d /tmp/android-command-line-tools \
    && mv /tmp/android-command-line-tools/cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm -rf /tmp/android-command-line-tools /tmp/android-command-line-tools.zip \
    && mkdir -p /root/.android \
    && touch /root/.android/repositories.cfg

RUN set +o pipefail \
    && yes | sdkmanager --licenses >/dev/null \
    && set -o pipefail \
    && sdkmanager \
        "build-tools;35.0.0" \
        "platform-tools" \
        "platforms;android-35" \
    && rm -rf /root/.android/cache "${ANDROID_HOME}/.temp"

WORKDIR /workspace

CMD ["./TvQuickMenu/gradlew", "-p", "TvQuickMenu", "--no-daemon", "assembleDebug", "lintDebug"]
