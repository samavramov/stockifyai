# Dockerfile (located in your backend directory)

# Stage 1: Build the Java application
# Using official OpenJDK 11 JDK for building (slim version for smaller size)
FROM openjdk:11-jdk-slim AS build
# Install Maven as it's not included in the slim JRE/JDK images by default
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Adjust this to compile and package your application
# Ensure your pom.xml generates the 'stockdashboard-backend-*-jar-with-dependencies.jar'
RUN mvn clean install -DskipTests

# Stage 2: Create the final lean runtime image
# Using official OpenJDK 11 JRE for a lighter runtime (slim version)
FROM openjdk:11-jre-slim
WORKDIR /app
# Copy the built JAR from the 'build' stage
# Make sure the filename matches what your Maven build produces
COPY --from=build /app/target/stockdashboard-backend-*-jar-with-dependencies.jar app.jar

# Expose the port your application runs on (assuming 8001)
EXPOSE 8001

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
