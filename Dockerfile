FROM gcr.io/distroless/java21-debian13:nonroot@sha256:e331c420616bf4771ac477c1c5b6cfc92de3d07a7d59bb15235bf2a7955ae881
ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/libs/*-*.jar ./
COPY build/libs/*.jar ./
EXPOSE 8080
USER 65532:65532
CMD ["app.jar"]
