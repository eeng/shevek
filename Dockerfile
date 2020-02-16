FROM clojure:openjdk-8-lein

# Install npm
RUN apt-get update -yq \
  && apt-get install curl gnupg -yq \
  && curl -sL https://deb.nodesource.com/setup_12.x | bash \
  && apt-get install nodejs -yq

WORKDIR /app

COPY project.clj .
RUN lein deps

COPY src src
COPY resources resources
# Optimus can only load the assets from the resources folder but npm installs them on the root folder
RUN ln -s ../node_modules resources/node_modules
RUN lein package

CMD ["java", "-jar", "target/uberjar/shevek.jar"]
