FROM eclipse-temurin:21-jre-jammy@sha256:dff6eb46527bd293a3c06bbbe9e64e8c9f91f01905b5abbdad31761c03059d2c
ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/libs/*-*.jar ./
COPY build/libs/*.jar ./
EXPOSE 8080
USER 65532:65532
CMD ["java", "-jar", "app.jar"]
