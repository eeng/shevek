# Shevek

TODO

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

## Development

Make sure to have Druid and MongoDB running on localhost, or point to their respective locations in the `dev/resources/config.edn` file, and then execute the following command on the project folder:

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

### Trying the production cljsbuild

```
lein build-frontend
lein run -m shevek.app/start
```

### Building from Source

```
bin/build.sh
```

### Acceptance Testing

To run the whole suite do:
```
lein test :acceptance
```

To run each test one at a time do:
```
lein repl :start :port 4101
```
Then connect to it, eval the `(ns)` and `(start-system)` forms, and finally eval the test blocks. It should run against the test database and web server.
