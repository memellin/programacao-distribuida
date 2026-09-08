FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/pulsealert-gateway-*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]