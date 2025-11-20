FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/wled-simulator-0.0.1-SNAPSHOT.jar /app/wled-simulator.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/wled-simulator.jar"]
