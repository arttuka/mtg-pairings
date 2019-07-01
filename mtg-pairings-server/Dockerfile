FROM adoptopenjdk/openjdk12-openj9:alpine AS builder
ENV LEIN_ROOT true
RUN apk --no-cache add curl bash \
    && curl -Lo /usr/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
    && chmod +x /usr/bin/lein
WORKDIR /app
COPY project.clj .
RUN lein deps
COPY ./scripts ./scripts
COPY ./src ./src
COPY ./env ./env
COPY ./resources ./resources
COPY ./test ./test
COPY ./test-resources ./test-resources
COPY decklist.cljs.edn pairings.cljs.edn ./
RUN lein test \
    && lein with-profile provided,prod do clean, garden once, minify-assets, fig:min pairings, fig:min decklist \
    && ./scripts/tag-assets.sh \
    && lein uberjar

FROM adoptopenjdk/openjdk12-openj9:alpine-jre
WORKDIR /app
COPY --from=builder /app/target/mtg-pairings.jar .
CMD ["java", "-jar", "mtg-pairings.jar"]
