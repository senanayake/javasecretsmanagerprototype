# 🔐 DDD Template: Java Secrets Access Library

## 📛 Bounded Context: `SecretAccess`

The `SecretAccess` context is responsible for retrieving, caching, refreshing, and exposing application secrets from secure secret stores like **CyberArk** and **AWS Secrets Manager**. It provides a consistent interface across various access methods and refresh strategies.

---

## 📘 Ubiquitous Language

| Term                     | Definition |
|--------------------------|------------|
| **Secret**               | A confidential value (e.g., password, API key, cert) needed by an application |
| **SecretProvider**       | An abstraction over CyberArk or AWS Secrets Manager |
| **AccessCredential**     | Credentials used to access the secrets (e.g., CyberArk Key, IAM Role) |
| **SecretReference**      | An object identifying a specific secret (e.g., name, store, version) |
| **SecretCache**          | Local memory or file-based cache to avoid excessive reads from the store |
| **RefreshPolicy**        | Strategy for keeping cached secrets up to date (e.g., polling, event-driven) |
| **SecretRollPair**       | Two secrets (active and inactive) for safe rollover |
| **SecretRefreshEvent**   | A domain event indicating a secret change notification or polling trigger |

---

## 🧱 Entities

### `Secret`
```java
Secret {
  id: String
  name: String
  value: char[]
  metadata: SecretMetadata
}
```

### `SecretMetadata`
```java
SecretMetadata {
  version: String
  lastRetrieved: Instant
  storeType: StoreType (AWS_SECRETS_MANAGER | CYBERARK)
  sourceRef: SecretReference
}
```

---

## 🧾 Value Objects

### `SecretReference`
```java
SecretReference {
  storeType: StoreType
  name: String
  versionHint: String (e.g., "latest", "v1", "inactive")
}
```

### `AccessCredential`
```java
AccessCredential {
  method: CredentialMethod (CYBERARK_API_KEY | IAM_ROLE)
  value: String | STSAssumeRoleConfig
}
```

---

## 🧩 Aggregates

### `SecretProviderAggregate`
Responsible for coordinating retrieval, refresh, and caching for a secret, including failover logic if needed.
```java
SecretProviderAggregate {
  reference: SecretReference
  provider: SecretProvider
  refreshPolicy: RefreshPolicy
  cache: SecretCache
  credential: AccessCredential

  getSecret()
  refreshSecret()
}
```

---

## 🏢 Domain Services

### `SecretResolverService`
- Accepts a `SecretReference` and `AccessCredential`
- Returns a `Secret`
- Applies caching, refresh policies, and rollover logic

### `SecretRefreshCoordinator`
- Polls or listens for changes
- Triggers refresh for applicable secrets

---

## 🪝 Domain Events

| Event Name               | Trigger                                | Outcome |
|--------------------------|----------------------------------------|---------|
| `SecretRefreshRequested`| Timer tick or external webhook         | Secret is refreshed |
| `SecretRefreshed`       | After secret value is updated          | Cache is updated |
| `SecretRolloverDetected`| Detected active/inactive switch        | Application notified or switched |

---

## 🧭 Policies & Rules

- ✅ Always return the active secret from an active/inactive pair
- 🔁 If polling is used, refresh secrets every `X` minutes (configurable)
- 📣 If event-driven, listen for AWS EventBridge or CyberArk notifications
- 🧪 Validate cache before falling back to store
- 🔐 All credential methods should support least-privilege principles

---

## 🏗️ Application Interfaces

### `SecretProvider` Interface
```java
public interface SecretProvider {
  Secret fetchSecret(SecretReference ref, AccessCredential credential);
  boolean supportsStore(StoreType type);
}
```

Implementations:
- `AwsSecretsManagerProvider`
- `CyberArkApiKeyProvider`
- `CyberArkIamRoleProvider`

---

### `SecretCache`
```java
public interface SecretCache {
  Optional<Secret> get(SecretReference ref);
  void put(Secret secret);
  void invalidate(SecretReference ref);
}
```

---

### `RefreshPolicy`
```java
public interface RefreshPolicy {
  void apply(SecretProviderAggregate secretProvider);
}
```

Implementations:
- `PollingRefreshPolicy`
- `EventDrivenRefreshPolicy`

---

## 🔄 Integration Points

| Component               | Integration |
|------------------------|-------------|
| **AWS Secrets Manager** | SDK integration with IAM-based auth |
| **CyberArk Vault**      | REST API (with Key or IAM Role) |
| **AWS EventBridge / SNS** | Event-driven refresh trigger |
| **Java Lambda Runtime** | Bootstrapped cache + fast cold start logic |

---

## ✅ Unit Tests To Drive Design

| Test Name                           | Purpose |
|------------------------------------|---------|
| `testGetSecretWithPollingRefresh()`| Ensures secrets are fetched and refreshed via polling |
| `testGetSecretWithEventNotification()`| Handles event-driven secret refresh |
| `testRolloverReturnsActive()`      | Tests active/inactive rollover handling |
| `testCyberArkFetchWithApiKey()`    | Validates CyberArk fetch via API Key |
| `testAWSFetchWithIamRole()`        | Validates AWS SM fetch via IAM |