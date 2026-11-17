FROM gcr.io/distroless/java21-debian13:nonroot@sha256:6b60e44d652d66a9e2e584364c96adac4df7133674b2834b6f71741b7cdb1ff3
ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/libs/*-*.jar ./
COPY build/libs/*.jar ./
EXPOSE 8080
USER 65532:65532
CMD ["app.jar"]
