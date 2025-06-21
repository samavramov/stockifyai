# Stage 1: Build the Java application
FROM maven:3.9-openjdk-21 AS build 
WORKDIR /app
COPY pom.xml .                   
COPY src ./src                   
RUN mvn clean install -DskipTests 

# Stage 2: Create the final lean runtime image
FROM openjdk:21-jre-slim         
WORKDIR /app
# Copy the built JAR from the 'build' stage
COPY --from=build /app/target/stockdashboard-backend-*-jar-with-dependencies.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"] # Run the application