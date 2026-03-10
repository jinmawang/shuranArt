FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/art-backend-1.0.0.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
