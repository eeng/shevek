# Shevek

Shevek is an interactive data exploration UI for Druid, aimed at end users.

## Features

TODO

## Requirements

* Java 1.8
* [MongoDB](https://www.mongodb.com/)

## Getting Started

1. [Download the latest release](https://github.com/eeng/shevek/releases/latest) and extract the package.

2. Edit the `config.edn` if necessary to point to your Druid and MongoDB hosts.

3. Change directory to the extracted folder and start the application:
```
java -Dconf=config.edn -jar shevek.jar
```

4. Open http://localhost:4000 in your browser, you should see the login page. Enter with user `admin` and password `secret123`.

## Contributing

We welcome everyone to contribute to this project. To do so, please read the following instructions:

First, make sure to have Druid and MongoDB running on localhost, or point to their respective locations in the `dev/resources/config.edn` file, and then execute the following command on the project folder:

```
lein cooper
```

After a while the UI should be accesible through http://localhost:4000.

Also, in the file `dev/clj/user.clj` there are some useful functions used during development.

### Connecting to the ClojureScript REPL

```
lein repl :connect :4002
user=> (cljs-repl)
```

### Building from Source

```
bin/build.sh
```

### Acceptance Testing

To run the whole acceptance tests suite do:
```
lein acceptance-tests
```

To run each test one at a time do:
```
lein acceptance-tests-repl
```
Then connect to it on port 4101, eval the `(start-system)` forms, and finally eval the test blocks. It should run against the test database and web server.
