FROM openjdk:11-slim AS builder
ENV LEIN_ROOT true
RUN apt-get update && apt-get install -y curl
RUN curl -Lo /usr/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein && chmod +x /usr/bin/lein
WORKDIR /app
COPY project.clj .
RUN lein deps
COPY ./src ./src
COPY ./env ./env
COPY ./resources ./resources
COPY prod.cljs.edn .
RUN lein do clean, uberjar

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /app/target/mtg-pairings.jar .
CMD ["java", "-jar", "mtg-pairings.jar"]
