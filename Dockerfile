FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY openapi ./openapi
COPY src ./src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre

ARG APP_UID=10001
ARG APP_GID=10001

ENV APP_HOME=/app \
	JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/urandom --enable-native-access=ALL-UNNAMED"

WORKDIR ${APP_HOME}

RUN groupadd --gid "${APP_GID}" app \
	&& useradd --uid "${APP_UID}" --gid "${APP_GID}" --home-dir "${APP_HOME}" --no-create-home --shell /usr/sbin/nologin app

COPY --from=build --chown=${APP_UID}:${APP_GID} --chmod=0444 /workspace/build/libs/*.jar app.jar

EXPOSE 8080

USER app:app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
