### Development

To try the prod cljsbuild do the following on separete tabs:
```
lein with-profile +prod cljsbuild auto prod
lein less4j auto
lein run -m shevek.app/start-for-dev
```
