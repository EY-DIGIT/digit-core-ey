
# FROM egovio/amazoncorretto:17-alpine3.19 AS build
FROM egovio/alpine-maven-builder-jdk-8:1-master-NA-6036091e AS build
ARG WORK_DIR
WORKDIR /app


# Install tools
RUN apk add --no-cache maven curl openssl ca-certificates

# --- START: Add Zscaler Root Certificate ---
# 1. Copy the Zscaler Root CA certificate into the container
# The source path is relative to your Dockerfile's location.
# If your Dockerfile is in the root of your project, and the cert is at build/maven/,
# then the source path should be build/maven/ZscalerRootCertificate-2048-SHA256-Feb2025.crt
COPY build/maven/ZscalerRootCertificate-2048-SHA256-Feb2025.pem /usr/local/share/ca-certificates/ZscalerRootCertificate-2048-SHA256-Feb2025.pem

# 2. Update system CA certificates (good practice, especially for other tools)
RUN update-ca-certificates

# 3. Import the Zscaler Root CA certificate into Java's default truststore (cacerts)
# THIS IS THE CRUCIAL BLOCK THAT NEEDS TO BE CORRECTED
RUN set -ex && \ 
    JAVA_BIN_PATH=$(readlink -f $(which java)) && \
    JAVA_HOME_FOR_KEYTOOL=$(dirname $(dirname "$JAVA_BIN_PATH")) && \
    # First attempt at CACERTS_PATH based on calculated JAVA_HOME
    CACERTS_PATH="$JAVA_HOME_FOR_KEYTOOL/lib/security/cacerts" && \
    # Fallback/alternative paths if the first one doesn't exist
    if [ ! -f "$CACERTS_PATH" ]; then \
        echo "Primary CACERTS_PATH ($CACERTS_PATH) not found. Checking alternatives..."; \
        if [ -f "/usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts" ]; then \
            CACERTS_PATH="/usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts"; \
        elif [ -f "/usr/lib/jvm/default-jvm/jre/lib/security/cacerts" ]; then \
            CACERTS_PATH="/usr/lib/jvm/default-jvm/jre/lib/security/cacerts"; \
        elif [ -f "$JAVA_HOME/lib/security/cacerts" ]; then \
            CACERTS_PATH="$JAVA_HOME/lib/security/cacerts"; \
        elif [ -f "/opt/ibm/java/jre/jre/lib/security/cacerts" ]; then \
            CACERTS_PATH="/opt/ibm/java/jre/jre/lib/security/cacerts"; \
        fi; \
    fi && \
    echo "Using JAVA_HOME for keytool: $JAVA_HOME_FOR_KEYTOOL" && \
    echo "Using CACERTS_PATH for keytool: $CACERTS_PATH" && \
    # Check if cacerts file exists before trying to modify it
    if [ ! -f "$CACERTS_PATH" ]; then \
        echo "Error: cacerts file NOT FOUND at $CACERTS_PATH. Aborting keytool operation." && exit 1; \
    fi && \
    echo "Listing existing certificates in the cacerts before import:" && \
    keytool -list -keystore "$CACERTS_PATH" -storepass changeit -v | grep 'Alias name: \|Valid from: \|Valid until:' || echo "Could not list existing certs (maybe empty or incorrect pass)" && \
    echo "Attempting to import Zscaler certificate..." && \
    keytool -import -trustcacerts -keystore "$CACERTS_PATH" -storepass changeit -noprompt -alias zscaler-root-ca -file /usr/local/share/ca-certificates/ZscalerRootCertificate-2048-SHA256-Feb2025.pem && \
    echo "Listing certificates after import:" && \
    keytool -list -keystore "$CACERTS_PATH" -storepass changeit -v | grep 'Alias name: \|Valid from: \|Valid until:' | grep 'zscaler-root-ca' && \
    echo "Zscaler certificate import step completed."
# --- END: Add Zscaler Root Certificate ---



# copy the project files
COPY ${WORK_DIR}/pom.xml ./pom.xml
COPY build/maven/start.sh ./start.sh

# not useful for stateless builds
# RUN mvn -B dependency:go-offline

COPY ${WORK_DIR}/src ./src
RUN mvn -B -f /app/pom.xml  clean package -Dmaven.test.skip=true




# Create runtime image
FROM egovio/8-openjdk-alpine
# FROM egovio/amazoncorretto:17-alpine3.19


WORKDIR /opt/egov

COPY --from=build /app/target/*.jar /app/start.sh /opt/egov/

RUN chmod +x /opt/egov/start.sh

CMD ["/opt/egov/start.sh"]

# FROM egovio/alpine-maven-builder-jdk-8:1-master-NA-6036091e AS build
# # FROM egovio/amazoncorretto:17-alpine3.19 AS build
# ARG WORK_DIR
# WORKDIR /app

# # Install Maven
# RUN apk add --no-cache maven

# # copy the project files
# COPY ${WORK_DIR}/pom.xml ./pom.xml
# COPY build/maven-java8/start.sh ./start.sh

# # not useful for stateless builds
# # RUN mvn -B dependency:go-offline

# COPY ${WORK_DIR}/src ./src
# RUN mvn -B -f /app/pom.xml package


# # Create runtime image
# FROM egovio/8-openjdk-alpine
# # FROM egovio/amazoncorretto:17-alpine3.19


# WORKDIR /opt/egov

# COPY --from=build /app/target/*.jar /app/start.sh /opt/egov/

# RUN chmod +x /opt/egov/start.sh

# CMD ["/opt/egov/start.sh"]