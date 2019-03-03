FROM openjdk:8-alpine AS builder
ENV LEIN_ROOT true
RUN apk --no-cache add curl bash
RUN curl -Lo /usr/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein && chmod +x /usr/bin/lein
WORKDIR /app
COPY project.clj .
RUN lein deps
COPY ./src ./src
COPY ./env ./env
COPY ./resources ./resources
COPY prod.cljs.edn .
RUN lein do clean, uberjar

FROM openjdk:8-alpine
WORKDIR /app
COPY --from=builder /app/target/mtg-pairings.jar .
CMD ["java", "-jar", "mtg-pairings.jar"]
