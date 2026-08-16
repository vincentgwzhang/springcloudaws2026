# ---------------------------
# Stage 1: Build
# ---------------------------
FROM amazoncorretto:25 AS builder

WORKDIR /build

RUN dnf install -y maven \
    && dnf clean all

COPY pom.xml ./

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests


# ---------------------------
# Stage 2: Runtime
# ---------------------------
FROM amazoncorretto:25-alpine

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]