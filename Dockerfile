FROM gcr.io/distroless/java21-debian12:nonroot
COPY /build/libs/amt-deltaker-bff-all.jar app.jar
CMD [ "-Xms256m", "-Xmx1024m", "-jar", "app.jar" ]
