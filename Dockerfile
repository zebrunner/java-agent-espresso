FROM gradle:7.4.0-jdk11

ENV ANDROID_HOME=/android-sdk
ENV ANDROID_CLI_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip"
ENV ANDROID_API_LEVEL="33"
ENV ANDROID_BUILD_TOOLS_VERSION="33.0.0"

ENV SIGNING_KEY=
ENV SIGNING_PASSWORD=

# Download android command line tools from remote repository
RUN mkdir -p /android-sdk/cmdline-tools && \
    wget ${ANDROID_CLI_TOOLS_URL} && \
    unzip *tools*linux*.zip -d /android-sdk/cmdline-tools && \
    mv /android-sdk/cmdline-tools/cmdline-tools /android-sdk/cmdline-tools/tools && \
    rm *tools*linux*.zip

ENV PATH ${PATH}:/android-sdk/cmdline-tools/latest/bin:/android-sdk/cmdline-tools/tools/bin:/android-sdk/platform-tools:/android-sdk/emulator

# Install appropriate Android SDK version
RUN yes | sdkmanager --licenses && sdkmanager "platforms;android-${ANDROID_API_LEVEL}" "build-tools;${ANDROID_BUILD_TOOLS_VERSION}"

COPY . /agent-espresso

WORKDIR /agent-espresso
