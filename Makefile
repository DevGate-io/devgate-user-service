DOCKER_COMPOSE = docker compose
DOCKER_DIR = ./docker
COMPOSE_FILE = $(DOCKER_DIR)/compose.yaml

up: down build-app
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) up --build -d

start: down
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) up -d

down:
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) down

build-app:
	./gradlew bootJar

seed:
	$(DOCKER_DIR)/seed-admin.sh