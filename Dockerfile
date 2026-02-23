FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
LABEL authors="sumit"
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY movement-service/pom.xml movement-service/

RUN mvn dependency:go-offline -B

COPY common/src common/src
COPY movement-service/src movement-service/src

RUN mvn clean package -DskipTests -pl movement-service -am

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

USER spring:spring

COPY --from=build /app/movement-service/target/movement-service-*.jar app.jar

EXPOSE 8081

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]