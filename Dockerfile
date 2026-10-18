# ---- build stage: full JDK + Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Resolve dependencies first so this layer is cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime stage: JRE only, non-root ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/target/fleettrack-*.jar app.jar
USER app
EXPOSE 8080
# Size the heap from the container's memory limit instead of the host's RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
