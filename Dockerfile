FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apk add --no-cache wget

RUN addgroup -S spring && adduser -S spring -G spring

COPY target/*.jar app.jar

RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="" \
    MAX_HEAP="384m" \
    MIN_HEAP="192m" \
    YOUNG_GEN="192m"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1


ENTRYPOINT ["sh", "-c", "java \
  -Xmx${MAX_HEAP} \
  -Xms${MIN_HEAP} \
  -Xmn${YOUNG_GEN} \
  -XX:+UseContainerSupport \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  ${JAVA_OPTS} \
  -jar app.jar"]