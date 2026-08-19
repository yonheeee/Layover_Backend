FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC -Dfile.encoding=UTF-8"

RUN addgroup -S layover && adduser -S layover -G layover

WORKDIR /app

COPY --from=build /workspace/target/Layover_Backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads && chown -R layover:layover /app

USER layover

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
