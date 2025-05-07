# Local Backend Setup Guide for Sarafrika Elimika

This documentation will guide you through setting up the Sarafrika Elimika backend locally using Docker. The setup includes:

1. PostgreSQL database
2. Keycloak 26.0.5 for authentication
3. Elimika backend service

## Prerequisites

- Docker and Docker Compose installed on your machine
- Basic knowledge of Docker and terminal commands
- Git (to clone the repository if needed)

## Step 1: Create a Docker Network

First, create the required external Docker network:

```bash
docker network create sarafrika
```

## Step 2: Set Up PostgreSQL

Create a PostgreSQL container with persistent volume:

```bash
# Create a volume for PostgreSQL data
docker volume create postgres_data

# Run PostgreSQL container
docker run -d \
  --name postgres \
  --network sarafrika \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=keycloak \
  -v postgres_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:13
```

This creates a PostgreSQL instance with:
- Username: postgres
- Password: postgres
- Default database: keycloak
- Port: 5432 (accessible from your host machine)

## Step 3: Set Up Keycloak 26.0.5

Create a Keycloak container connected to the PostgreSQL database:

```bash
# Run Keycloak container
docker run -d \
  --name keycloak \
  --network sarafrika \
  -e KC_DB=postgres \
  -e KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak \
  -e KC_DB_USERNAME=postgres \
  -e KC_DB_PASSWORD=postgres \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_HOSTNAME=localhost \
  -p 8085:8080 \
  quay.io/keycloak/keycloak:26.0.5 start-dev
```

This starts Keycloak with:
- Admin user: admin
- Admin password: admin
- Web interface accessible at: http://localhost:8085
- Connected to the PostgreSQL container we created earlier

## Step 4: Configure Keycloak

1. Access the Keycloak Admin Console at http://localhost:8085
2. Log in with admin/admin
3. Create a new realm named `sarafrika` (or your preferred realm name)
4. Create a client for the Elimika backend:
   - Client ID: `elimika-backend`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Valid Redirect URIs: `http://localhost:8080/*`
   - Web Origins: `http://localhost:8080`
5. After saving, go to the Credentials tab for this client and note the Secret

## Step 5: Create Environment Variables File

Create a `.env` file in your project directory with the following variables:

```
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/elimika
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Keycloak Configuration
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:8080/realms/sarafrika
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/realms/sarafrika/protocol/openid-connect/certs
APP_KEYCLOAK_REALM=sarafrika
APP_KEYCLOAK_SERVER_URL=http://keycloak:8080
APP_KEYCLOAK_ADMIN_CLIENTID=admin-cli
APP_KEYCLOAK_ADMIN_CLIENTSECRET=[YOUR_ADMIN_CLI_SECRET]

# Encryption Configuration
ENCRYPTION_SECRET_KEY=your-secret-encryption-key
ENCRYPTION_SALT=your-encryption-salt
```

Replace `[YOUR_ADMIN_CLI_SECRET]` with the client secret from Keycloak.

## Step 6: Create a Docker Compose File

Create a file named `docker-compose.yml` with the following content (which combines your existing Elimika service configuration with the new services):

```yaml
version: '3'

networks:
  sarafrika:
    external: true

volumes:
  elimika_uploads:
    name: elimika_uploads
  postgres_data:
    external: true

services:
  postgres:
    image: postgres:13
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${SPRING_DATASOURCE_USERNAME}
      POSTGRES_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      POSTGRES_DB: elimika
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - sarafrika
    ports:
      - "5432:5432"
      
  keycloak:
    image: quay.io/keycloak/keycloak:26.0.5
    restart: unless-stopped
    command: start-dev
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      KC_DB_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HOSTNAME: localhost
    ports:
      - "8085:8080"
    networks:
      - sarafrika
    depends_on:
      - postgres
      
  elimika:
    image: sarafrika/elimika:latest
    restart: unless-stopped
    environment:
      SERVER_PORT: 30000
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: ${SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI}
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: ${SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI}
      APP_KEYCLOAK_REALM: ${APP_KEYCLOAK_REALM}
      APP_KEYCLOAK_SERVERURL: ${APP_KEYCLOAK_SERVER_URL}
      APP_KEYCLOAK_ADMIN_CLIENTID: ${APP_KEYCLOAK_ADMIN_CLIENTID}
      APP_KEYCLOAK_ADMIN_CLIENTSECRET: ${APP_KEYCLOAK_ADMIN_CLIENTSECRET}
      ENCRYPTION_SECRET_KEY: ${ENCRYPTION_SECRET_KEY}
      ENCRYPTION_SALT: ${ENCRYPTION_SALT}
    ports:
      - "8080:30000"
    volumes:
      - elimika_uploads:/app/data
    networks:
      - sarafrika
    depends_on:
      - postgres
      - keycloak
```

## Step 7: Start the Services

Start all services using Docker Compose:

```bash
docker-compose up -d
```

This will start PostgreSQL, Keycloak, and the Elimika backend.

## Step 8: Verify the Setup

1. Check if all containers are running:
   ```bash
   docker-compose ps
   ```

2. Access Keycloak at http://localhost:8085

3. Access the Elimika backend at http://localhost:8080

## Troubleshooting

### Database Connection Issues
- Make sure PostgreSQL is running and accessible
- Verify the database credentials in the `.env` file
- Check the logs for database connection errors:
  ```bash
  docker-compose logs postgres
  docker-compose logs elimika
  ```

### Keycloak Issues
- If Keycloak fails to start, check its logs:
  ```bash
  docker-compose logs keycloak
  ```
- Ensure the PostgreSQL container is fully initialized before Keycloak tries to connect

### Network Issues
- Make sure the `sarafrika` network exists:
  ```bash
  docker network ls | grep sarafrika
  ```
- Verify all containers are connected to the network:
  ```bash
  docker network inspect sarafrika
  ```

## Stopping the Services

To stop all services:

```bash
docker-compose down
```

To stop and remove volumes (this will delete all data):

```bash
docker-compose down -v
```

## Additional Information

### Accessing Logs
```bash
# All logs
docker-compose logs

# Service-specific logs
docker-compose logs elimika
docker-compose logs keycloak
docker-compose logs postgres
```

### Running Specific Services
If you want to run only specific services:

```bash
docker-compose up -d postgres keycloak
```

### Rebuilding the Environment
To rebuild the environment from scratch:

```bash
docker-compose down -v
docker volume rm postgres_data elimika_uploads
docker network rm sarafrika
# Then follow the setup steps again
```
