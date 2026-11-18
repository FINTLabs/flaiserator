FROM busybox:1.37.0 AS application
WORKDIR /www
RUN printf 'flaiserator-system-test\n' > index.html
EXPOSE 8080
CMD ["httpd", "-f", "-p", "8080"]

FROM busybox:1.37.0 AS job
CMD ["sh", "-c", "printf 'flaiserator-system-test-job-completed\n'"]
