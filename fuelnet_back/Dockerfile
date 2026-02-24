FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

COPY target/fuelnet-0.0.1.jar app.jar

EXPOSE 3015

ENTRYPOINT ["java", "-jar", "app.jar"]
