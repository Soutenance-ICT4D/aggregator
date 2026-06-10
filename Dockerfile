# ============================================================
# STAGE 1 - Build
# Compile le projet Spring Boot avec Maven
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copie du wrapper Maven et du pom.xml en premier
# pour bénéficier du cache Docker lors des builds suivants
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copie des sources et build sans tests
COPY src/ src/
RUN ./mvnw package -DskipTests -q


# ============================================================
# STAGE 2 - Production
# Image finale légère avec uniquement le JRE
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Port par défaut - surchargeable via docker-compose (environment: SPRING_SERVER_PORT)
ENV SPRING_SERVER_PORT=3001

# Utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copie uniquement le JAR compilé depuis le stage précédent
COPY --from=builder /build/target/*.jar app.jar

# Port documentaire uniquement - le vrai binding est dans docker-compose (ports:)
EXPOSE 3001

# Options JVM optimisées pour les containers :
# -XX:+UseContainerSupport  → détecte les limites mémoire du container
# -XX:MaxRAMPercentage=75.0 → utilise 75% de la RAM allouée au container
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]