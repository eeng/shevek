# Shevek

Shevek is an interactive data exploration UI for Druid, aimed at end users.

## Features

* Easy to use report designer. End users can build their own reports without any knowledge of a query language, by using drag and drop in a similar way to pivot tables in Excel.
* Automatic chart generation.
* Multiple dashboards support. Related reports can be grouped into dashboards to provide at-a-glance views of key performance indicators.
* Multi-user support. Each user can have their own reports and dashboards.
* Fine-grained authorization. An administrator can configure which cubes, dimensions or measures a user is allowed to view. It's also possible to define filters based on dimension values (e.g. when a user can only view data from certain region).
* Possibility to share dashboards or individual reports between users.
* Automatic discovery of Druid datasources. Upon start, the application will search the available cubes and its dimensions and measures, which can be customized afterwards.

*WARNING: Please bear in mind this software is still in **beta status**. It's actively used in production, but it is under development and could change at any time.*

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

This command it's a convenient alias to start both the backend (Clojure) and frontend (ClojureScript with Figwheel and less support) in the same terminal window.

### Connecting to the Clojure REPL (backend)

You can connect to the nREPL server (port 4001 by default) with your favorite editor to evaluate expressions on the running backend. In the file `dev/clj/user.clj` there are some useful functions used during development.

```
lein repl :connect :4001
```

### Connecting to the ClojureScript REPL (frontend)

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
