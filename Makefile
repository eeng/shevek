DEMO_COMPOSE_OPTS = -f docker-compose.yml -f docker-compose.demo.yml -p shevek_demo
CI_COMPOSE_OPTS = -f docker-compose.yml -f docker-compose.ci.yml -p shevek_ci

demo.start:
	docker-compose ${DEMO_COMPOSE_OPTS} up ${ARGS}

demo.stop:
	docker-compose ${DEMO_COMPOSE_OPTS} down ${ARGS}

demo.druid.seed:
	docker-compose ${DEMO_COMPOSE_OPTS} exec coordinator cat quickstart/tutorial/wikipedia-index.json \
		| curl -X 'POST' -H 'Content-Type:application/json' -d @- http://localhost:8081/druid/indexer/v1/task

ci:
	docker-compose ${CI_COMPOSE_OPTS} up --abort-on-container-exit --exit-code-from shevek  ${ARGS}
