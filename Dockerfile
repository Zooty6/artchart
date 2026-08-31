FROM eclipse-temurin:25-jdk AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN ./mvnw --batch-mode clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /build/target/artcharts-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
