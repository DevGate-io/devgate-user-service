DOCKER_COMPOSE = docker compose
COMPOSE_FILE = ./compose.yaml

up: down
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) up --build -d

start: down
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) up -d

down:
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) down

seed:
	./seed-admin.sh