DOCKER_COMPOSE = docker compose
COMPOSE_FILE = ./compose.yaml

up: down
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) up -d

down:
	$(DOCKER_COMPOSE) -f $(COMPOSE_FILE) down