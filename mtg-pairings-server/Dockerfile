FROM adoptopenjdk/openjdk12-openj9:alpine-jre
WORKDIR /app
COPY ./target/mtg-pairings.jar .
CMD ["java", "-jar", "mtg-pairings.jar"]
