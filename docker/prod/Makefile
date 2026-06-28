DOCKER_COMPOSE = docker compose
COMPOSE_FILE = ./compose.yaml

up: down build-app
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) up --build -d

start: down
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) up -d

down:
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) down

build-app:
	cd ../.. && ./gradlew bootJar

seed:
	./seed-admin.sh