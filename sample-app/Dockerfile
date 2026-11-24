FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/sample-app-1.0.0-all.jar app.jar
EXPOSE 7070
# Non-root user
RUN useradd -r -u 10001 appuser
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
