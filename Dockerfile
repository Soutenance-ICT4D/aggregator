# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build (Maven + JDK 21)
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copie du wrapper Maven et du pom.xml en premier pour bénéficier du cache
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copie des sources et build (sans tests)
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Image de production (JRE 21 uniquement)
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# Options JVM recommandées pour containers :
# -XX:+UseContainerSupport  — détecte les limites mémoire du container
# -XX:MaxRAMPercentage=75.0 — utilise 75% de la RAM allouée au container
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
