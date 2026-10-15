ARG IMAGE_ACR=docker.io

# Stage 1: source of the Java truststore with the Defra certificates (base image mirrored into
# ACR by the pipeline)
FROM ${IMAGE_ACR}/defradigital/java:jre-21 AS builder

# ==============================================================================
# Stage 2: Hardened Runtime (Google Distroless)
# ==============================================================================
FROM gcr.io/distroless/java21-debian13:nonroot
WORKDIR /app

# Copy Java cacerts containing Defra certificates from builder (the JRE's default truststore)
COPY --from=builder /opt/java/openjdk/lib/security/cacerts /etc/ssl/certs/java/cacerts

# Copy Azure Application Insights agent & config
COPY target/agent-libs/applicationinsights-agent.jar ./applicationinsights-agent.jar
COPY lib/applicationinsights.json ./applicationinsights.json

# Copy the packaged Azure Function output produced by `mvn package`: the dependencies, and the
# function jar on its own so the agent jar stays off the classpath
COPY target/azure-functions/notify-azure-function/lib/ ./lib/
COPY target/azure-functions/notify-azure-function/*.jar ./bin/

# Numeric UID/GID (distroless 'nonroot') so runAsNonRoot can verify it without runAsUser
USER 65532:65532

# Processes the notify queue once, as a KEDA ScaledJob
ENTRYPOINT ["java", \
            "-javaagent:/app/applicationinsights-agent.jar", \
            "-cp", "/app/lib/*:/app/bin/*", \
            "uk.gov.defra.tracesx.notify.KedaJobRunner"]
