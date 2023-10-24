FROM gcr.io/distroless/java17-debian11
COPY /build/libs/amt-deltaker-bff-all.jar app.jar
CMD [ "-Xms256m", "-Xmx1024m", "-jar", "app.jar" ]
