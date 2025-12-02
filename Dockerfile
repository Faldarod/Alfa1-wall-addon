FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/alfa-wall-addon-0.0.1-SNAPSHOT.jar /app/alfa-wall-addon.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/alfa-wall-addon.jar"]
