# Etapa 1: compilación con Maven Wrapper
FROM docker.io/library/eclipse-temurin:21-jdk-alpine AS construccion
WORKDIR /fuente
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests -Djacoco.skip=true \
    && cp target/reservas-servicios-*.jar /fuente/app.jar

# Etapa 2: imagen de ejecución mínima con usuario sin privilegios
FROM docker.io/library/eclipse-temurin:21-jre-alpine
RUN addgroup -S reservas && adduser -S -G reservas reservas
WORKDIR /app
COPY --from=construccion --chown=reservas:reservas /fuente/app.jar app.jar
USER reservas
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
