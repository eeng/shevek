## Development

Make sure to have Druid and MongoDB running on localhost or point to their respective locations in the config.edn file, and then execute the following command on the project folder:

```
lein cooper
```

After a while the UI should be accesible through http://localhost:4000.

### Connecting to the ClojureScript REPL

```
lein repl :connect :4002
user=> (cljs-repl)
```

### Trying the production cljsbuild

```
lein build-frontend
lein run -m shevek.app/start-for-dev
```
