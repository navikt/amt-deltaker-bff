FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app

COPY build/install/amt-deltaker-bff /app

ENV TZ="Europe/Oslo"
EXPOSE 8080

ENTRYPOINT ["/usr/bin/java"]
CMD ["-Dlogback.configurationFile=deltaker-bff-logback.xml", "-Xms256m", "-Xmx1024m", "-cp", "lib/*", "no.nav.amt.deltaker.bff.ApplicationKt"]