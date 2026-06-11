# Infra BDD

## Feature: MySQL Local Runtime

### Scenario: Developer starts MySQL

Given Docker is installed
When `infra/scripts/mysql-up.ps1 -ApplySchema` runs
Then the `margins-mysql` container becomes healthy
And `db/schema/001_create_mvp_schema.sql` is applied
And `db/seed/001_seed_mvp_data.sql` initializes development data

### Scenario: Developer stops MySQL

Given the `margins-mysql` container is running
When `infra/scripts/mysql-down.ps1` runs
Then the local MySQL container is stopped
And local data remains available for the next start

### Scenario: Developer removes MySQL data

Given local MySQL data should be reset completely
When `infra/scripts/mysql-down.ps1 -Volumes` runs
Then the local MySQL container is stopped
And the Docker volume is removed

## Feature: Raspberry Pi Deployment

### Scenario: Main branch deploys build artifacts

Given changes are merged to `main`
When the deployment workflow runs
Then frontend and backend artifacts are built
And artifacts are transferred to Raspberry Pi
And the running service is restarted with the new version

## Feature: Future Compose Migration

### Scenario: Front/back are added to Docker Compose later

Given MySQL already runs through Docker
When front/back services are containerized
Then the compose structure can include them without moving domain files
