## Development

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
