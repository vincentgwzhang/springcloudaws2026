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
COPY opentelemetry-javaagent.jar /opt/opentelemetry-javaagent.jar

# Load the upstream OpenTelemetry agent for every container start. These are
# image defaults and can be overridden with docker run -e or Docker Compose.
ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/opentelemetry-javaagent.jar" \
    OTEL_SERVICE_NAME="awsdemo1" \
    OTEL_PROPAGATORS="tracecontext,baggage,xray" \
    OTEL_TRACES_EXPORTER="none" \
    OTEL_METRICS_EXPORTER="none" \
    OTEL_LOGS_EXPORTER="none"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
