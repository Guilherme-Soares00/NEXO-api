FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src/main ./src/main
RUN mvn -B -ntp -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S -g 10001 nexo && adduser -S -D -H -u 10001 -G nexo nexo
WORKDIR /app
COPY --from=build --chown=nexo:nexo /build/target/nexo-0.0.1-SNAPSHOT.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0 SERVER_PORT=8080
USER nexo:nexo
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]
