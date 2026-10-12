# Etapa 1: compilar con el wrapper (no hace falta tener Maven instalado)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Etapa 2: solo el JRE y el jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/vtex-payment-connector-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
