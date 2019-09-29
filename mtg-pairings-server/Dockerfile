FROM adoptopenjdk/openjdk12-openj9:alpine AS builder
ENV LEIN_ROOT true
RUN apk --no-cache add curl bash nodejs npm \
    && curl -Lo /usr/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
    && chmod +x /usr/bin/lein \
    && npm i -g uglifycss
WORKDIR /app
COPY project.clj .
RUN lein deps
COPY ./src ./src
COPY ./env ./env
COPY ./dev-resources ./dev-resources
COPY ./resources ./resources
COPY ./test ./test
COPY ./test-resources ./test-resources
COPY decklist.cljs.edn pairings.cljs.edn ./
RUN lein do \
         test, \
         kibit, \
         eastwood, \
         cljfmt check \
    && lein with-profile provided,prod do \
         clean, \
         garden once \
    && uglifycss target/public/css/main.css > target/public/css/main.min.css \
    && lein with-profile provided,prod do \
         fig:min pairings, \
         fig:min decklist, \
         buster, \
         uberjar

FROM adoptopenjdk/openjdk12-openj9:alpine-jre
WORKDIR /app
COPY --from=builder /app/target/mtg-pairings.jar .
CMD ["java", "-jar", "mtg-pairings.jar"]
