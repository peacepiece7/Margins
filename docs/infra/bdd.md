# Infra BDD

## Feature: MySQL Local Runtime

### Scenario: Developer starts MySQL

Given Docker is installed
When the MySQL startup script runs
Then MySQL is available to the backend
And seed scripts can initialize development data

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
