# STAGE 1: Build shared libraries (common-contracts, common-security)
FROM maven:3.9.6-eclipse-temurin-17 AS shared-libs-builder
LABEL stage=shared-libs-builder

WORKDIR /build

# Copy the DEDICATED Docker build parent POM for shared libraries
# This pom-docker.xml should ONLY list common-contracts and common-security as modules.
COPY pom-docker.xml ./pom-docker.xml

# Copy ONLY the poms and src for modules defined in pom-docker.xml
# IMPORTANT: common-contracts/pom.xml and common-security/pom.xml on your filesystem
# MUST have their <parent> section refer to:
#   <artifactId>project-management-docker-build-parent</artifactId>
#   <relativePath>../pom-docker.xml</relativePath>
COPY common-contracts/pom.xml ./common-contracts/
COPY common-security/pom.xml ./common-security/
COPY common-contracts/src ./common-contracts/src
COPY common-security/src ./common-security/src

# Build and install shared modules to local .m2 repository.
# Maven uses /build/pom-docker.xml as the root.
# Since pom-docker.xml only lists common-contracts and common-security,
# Maven will build exactly these two.
RUN mvn clean install -f pom-docker.xml -DskipTests -B

# STAGE 2: Build the api-gateway application
FROM maven:3.9.6-eclipse-temurin-17 AS app-builder
LABEL stage=app-builder

WORKDIR /build

# Copy the local Maven repository from the shared-libs-builder stage.
# This contains the compiled .jar files for common-contracts and common-security.
COPY --from=shared-libs-builder /root/.m2 /root/.m2

# Copy the MAIN project parent POM (the one that lists ALL modules).
# IMPORTANT: This is your actual pom.xml from the project root.
COPY pom.xml ./pom.xml

# Copy the specific service's POM and source code.
# IMPORTANT: api-gateway/pom.xml on your filesystem MUST have its <parent> section refer to:
#   <artifactId>project-management-parent</artifactId> (your main parent)
#   <relativePath>../pom.xml</relativePath> (pointing to the main pom.xml)
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src

# Copy the POMs of the shared libraries again. This helps Maven understand the
# full reactor if needed, though the actual artifacts will be pulled from the .m2.
COPY common-contracts/pom.xml ./common-contracts/
COPY common-security/pom.xml ./common-security/
# If api-gateway directly depended on another *service* module AND you wanted to build
# them together with -am from the main pom.xml, you'd copy that service's pom.xml too.
# However, it's generally cleaner to build each service independently in its Dockerfile.

# Build the api-gateway application.
# Maven uses /build/pom.xml (your main project parent) as the root.
# It will find common-contracts and common-security in the .m2 repo copied earlier.
# Using -f to target the specific service POM is robust.
RUN mvn clean package -f api-gateway/pom.xml -DskipTests -B
# Alternative if you want to use the main pom.xml as the execution root for this module:
# RUN mvn clean package -pl :api-gateway -am -DskipTests -B
# The -pl :api-gateway syntax is used when the command is run from the parent POM's directory.
# The -am might be needed if api-gateway has inter-module dependencies that are also part of the main reactor
# and not yet in .m2 (but common ones are). For simplicity and robustness with pre-built common libs,
# -f api-gateway/pom.xml is often preferred.

# STAGE 3: Runtime stage
FROM eclipse-temurin:17-jre-alpine
LABEL stage=runtime

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --from=app-builder /build/api-gateway/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]