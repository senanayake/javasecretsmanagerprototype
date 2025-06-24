# Java Secrets Access Library

A domain-driven design (DDD) approach to secrets management in Java applications.

## Overview

This library provides a consistent interface for accessing secrets from different secret stores such as AWS Secrets Manager and CyberArk. It follows Domain-Driven Design principles to create a clean separation of concerns and flexible architecture.

### Key Features

- Clean domain model with value objects and entities
- Pluggable secret providers (AWS Secrets Manager, CyberArk, etc.)
- Multiple refresh strategies (polling, event-driven)
- In-memory caching with configurable TTL
- Comprehensive event system for monitoring secrets lifecycle
- Thread-safe implementation
- Extensive test coverage

## Architecture

### DDD Layers

The library is organized into the following layers:

1. **Domain Layer**: Core business logic and models
   - Value Objects: `SecretReference`, `AccessCredential`, etc.
   - Entities: `Secret`
   - Domain Events: `SecretRefreshed`, `SecretRefreshRequested`, etc.
   - Domain Services: `SecretResolverService`, `RefreshPolicy`, etc.

2. **Application Layer**: Orchestration and use cases
   - Application Services: `DefaultSecretResolverService`, `DefaultSecretRefreshCoordinator`
   - Ports: `EventPublisher`

3. **Infrastructure Layer**: Technical implementations
   - Adapters: `MockSecretProvider`, `InMemorySecretCache`
   - Event Publishers: `InMemoryEventPublisher`

4. **API Layer**: Client-facing interfaces
   - Facades: `SecretManager`

### Component Diagram

```
┌─────────────────┐      ┌───────────────────────┐
│                 │      │                       │
│  SecretManager  │─────▶│  SecretResolverService│
│     (API)       │      │    (Application)      │
│                 │      │                       │
└────────┬────────┘      └──────────┬────────────┘
         │                          │
         │                          ▼
         │               ┌────────────────────┐
         │               │                    │
         └─────────────▶ │   SecretProvider   │
                         │     (Domain)       │
                         │                    │
                         └────────┬───────────┘
                                  │
                                  ▼
                         ┌────────────────────┐
                         │                    │
                         │   Secret Stores    │
                         │ (External Systems) │
                         │                    │
                         └────────────────────┘
```

## Usage

### Basic Usage

```java
// Create components for the SecretManager
MockSecretProvider awsProvider = MockSecretProvider.builder()
        .withStoreType(StoreType.AWS_SECRETS_MANAGER)
        .build();

InMemorySecretCache cache = new InMemorySecretCache();

InMemoryEventPublisher eventPublisher = new InMemoryEventPublisher();

// Build the SecretManager
try (SecretManager secretManager = SecretManager.builder()
        .withProvider(awsProvider)
        .withCache(cache)
        .withEventPublisher(eventPublisher)
        .withDefaultCacheTTL(Duration.ofMinutes(15))
        .build()) {
    
    // Define a secret reference
    SecretReference dbCredRef = new SecretReference(
            StoreType.AWS_SECRETS_MANAGER,
            "database/credentials",
            "latest");
    
    // Define credentials to access the secret store
    AccessCredential awsCredential = AccessCredential.forIamRole(
            STSAssumeRoleConfig.builder()
                    .withRoleArn("arn:aws:iam::123456789012:role/SecretsAccessRole")
                    .withRoleSessionName("app-session")
                    .build());
    
    // Register the secret
    secretManager.registerSecret(
            "db-creds",
            dbCredRef,
            awsCredential,
            null); // No refresh policy
    
    // Get the secret
    Secret dbSecret = secretManager.getSecret("db-creds");
    
    // Use the secret value (safely)
    try (Secret.AutoClearingSecret autoSecret = new Secret.AutoClearingSecret(dbSecret)) {
        char[] secretValue = autoSecret.getSecret().getValue();
        // Use secretValue...
    } // Secret value automatically cleared from memory
}
```

### Adding a Refresh Policy

```java
// Create a polling refresh policy
MockPollingRefreshPolicy refreshPolicy = new MockPollingRefreshPolicy(
        Duration.ofMinutes(5),
        eventPublisher::publish);

// Register the secret with the refresh policy
secretManager.registerSecret(
        "db-creds",
        dbCredRef,
        awsCredential,
        refreshPolicy);
```

### Event Listeners

```java
// Subscribe to secret events
eventPublisher.subscribe(SecretRefreshed.class, event -> {
    System.out.println("Secret refreshed: " + event.getSecretReference().getName());
});

eventPublisher.subscribe(SecretRolloverDetected.class, event -> {
    System.out.println("Secret rollover detected: " + event.getSecretReference().getName());
});
```

## Project Structure

- **domain**: Core domain model, interfaces, and value objects
- **application**: Application services and ports
- **infrastructure**: Technical implementations and adapters
- **api**: Client-facing API and facades
- **examples**: Example usage

## Requirements

- Java 17+
- Maven 3.8+

## Building

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

## Future Enhancements

- Real AWS Secrets Manager integration
- CyberArk integration
- HashiCorp Vault integration
- Distributed cache with Redis
- Spring Boot integration
- Encryption for cached secrets
- Admin REST API for refresh operations
- Telemetry metrics
- Dynamic secret rotation support

## License

Apache License 2.0
