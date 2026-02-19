FROM openjdk:17-jdk-slim
LABEL authors="sumit"

WORKDIR /app

COPY target/warehouse-service-1.0.0.jar warehouse-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "warehouse-service.jar"]