FROM eclipse-temurin:17-jdk-jammy
LABEL authors="sumit"

WORKDIR /app

COPY target/warehouse-service-1.0.0.jar warehouse-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "warehouse-service.jar"]