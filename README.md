# Shevek

Shevek is an interactive data exploration UI for Druid, aimed at end-users.

## Features

- **Easy to use report designer**. End-users can build their own reports without any knowledge of a query language, by using drag and drop in a similar way to pivot tables in Excel.
- Automatic simple **charts** generation.
- **Multiple dashboards**. Related reports can be grouped into dashboards to provide at-a-glance views of key performance indicators.
- **Multi-user** support. Each user can have their own reports and dashboards.
- **Fine-grained authorization**. An administrator can configure which cubes, dimensions or measures a user is allowed to see. It's also possible to define filters based on dimension values (e.g. when a user can only view data from a certain region).
- Possibility to **share dashboards or individual reports** between users.
- **Automatic discovery of Druid data sources**. Upon start, the application will search the available cubes and its dimensions and measures, which can be customized afterward.

_WARNING: Please bear in mind this software is still in **beta status**. It's actively used in production, but it is under development and could change at any time._

## Quickstart

If you just want to try the application, the fastest way to set it up is with [Docker](https://www.docker.com/). It'll download all the requirements, install a minimal Druid cluster, and finally start Shevek so you can query its data.

_NOTE: Druid is quite heavy because it starts several JVM processes, so make sure Docker has at least 4GB available._

Clone the repo, open the folder in a terminal, and then execute:

```sh
docker-compose up
```

Go grab a cup of coffee while Docker does its thing. After a while, if you see the following messages it means everything went well:

> Starting web server on http://localhost:4000

> Schema seeding done

Now, let's load the sample data provided by Druid to have something to play with:

```sh
docker-compose exec coordinator cat quickstart/tutorial/wikipedia-index.json | curl -X 'POST' -H 'Content-Type:application/json' -d @- http://localhost:8081/druid/indexer/v1/task
# => {"task":"index_wikipedia..."}
```

The task usually takes a while to complete. You can monitor its progress in the Druid Console at http://localhost:8888/. When it's completed, check also that the wikipedia datasource is fully available.

Finally, you can head to http://localhost:4000 and you should see the Shevek login screen. Enter with user `admin` and password `secret123`.

## Installation

To use the application without Docker, you need the following requirements:

- Java 1.8 _(not tested yet on newer versions)_.
- [MongoDB](https://www.mongodb.com/). Upon start, a `shevek` database will be created here.
- A [Druid](https://druid.apache.org/) cluster with some data, of course.

Now, follow these instructions:

1. [Download the latest release](https://github.com/eeng/shevek/releases/latest) and extract the package.

2. Edit the `config.edn` if necessary to point to your Druid and MongoDB hosts.

3. Change directory to the extracted folder and start the application:

```
java -Dconf=config.edn -jar shevek.jar
```

4. Open http://localhost:4000 in your browser, you should see the login page. Enter with user `admin` and password `secret123`.

## Contributing

We welcome everyone to contribute to this project. To do so, please read the following instructions:

1. Copy `dev/resources/config.edn.example` to `dev/resources/config.edn`

2. Make sure you have Druid and MongoDB running on localhost, or point to their respective locations in the previous file.

3. Execute the following command on the project folder: `lein cooper`
   This command is a convenient alias to start both the backend (Clojure) and frontend (ClojureScript with Figwheel and less support) in the same terminal window.

4. After a while, the UI should be accessible through http://localhost:4000.

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
