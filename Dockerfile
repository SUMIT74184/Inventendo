# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Step 1: Copy only the pom.xml to cache dependencies (Faster builds!)
COPY pom.xml .
RUN mvn dependency:go-offline

# Step 2: Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Step 3: Copy the jar from the build stage
# Using a wildcard (*) is fine, but ensure only one JAR exists in target/
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

# Step 4: Use the EXACT filename used in the COPY command above
ENTRYPOINT ["java", "-jar", "app.jar"]