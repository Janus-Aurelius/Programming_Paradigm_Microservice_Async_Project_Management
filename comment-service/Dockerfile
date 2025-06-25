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
# Maven uses /build/pom.xml (which is pom-docker.xml) as the root.
# Since pom-docker.xml only lists common-contracts and common-security,
# Maven will build exactly these two.
RUN mvn clean install -f pom-docker.xml -DskipTests -B

# STAGE 2: Build the comment-service application
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
# IMPORTANT: comment-service/pom.xml on your filesystem MUST have its <parent> section refer to:
#   <artifactId>project-management-parent</artifactId> (your main parent)
#   <relativePath>../pom.xml</relativePath> (pointing to the main pom.xml)
COPY comment-service/pom.xml ./comment-service/
COPY comment-service/src ./comment-service/src

# Copy the POMs of the shared libraries again. This helps Maven understand the
# full reactor if needed, though the actual artifacts will be pulled from the .m2.
COPY common-contracts/pom.xml ./common-contracts/
COPY common-security/pom.xml ./common-security/

# Build ONLY comment-service. We use -f to specify the main parent pom, and -pl to select
# the specific module. Maven will automatically use common-contracts and common-security
# from the .m2 repository (installed in stage 1).
RUN mvn clean package -f comment-service/pom.xml -DskipTests -B

# STAGE 3: Runtime stage
FROM eclipse-temurin:17-jre-alpine
LABEL stage=runtime

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --from=app-builder /build/comment-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8088

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
