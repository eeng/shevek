## Development

Make sure to have Druid and MongoDB running on localhost or point to their respective locations in the config.edn file, and then execute the following command on the project folder:

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

### Packaging

```
lein package
java -Dconf=path/to/config.edn -jar target/uberjar/shevek.jar
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
