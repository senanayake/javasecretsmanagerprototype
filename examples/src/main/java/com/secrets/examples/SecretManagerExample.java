package com.secrets.examples;

import com.secrets.api.SecretManager;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.CredentialMethod;
import com.secrets.domain.model.STSAssumeRoleConfig;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.infrastructure.cache.InMemorySecretCache;
import com.secrets.infrastructure.event.InMemoryEventPublisher;
import com.secrets.infrastructure.provider.mock.MockPollingRefreshPolicy;
import com.secrets.infrastructure.provider.mock.MockSecretProvider;

import java.time.Duration;
import java.util.Scanner;

/**
 * Example demonstrating how to use the Java Secrets Library.
 * This example sets up a fully functioning SecretManager with mock implementations
 * and demonstrates key operations such as retrieving and refreshing secrets.
 */
public class SecretManagerExample {

    public static void main(String[] args) {
        try {
            // Create an in-memory event publisher for tracking events
            InMemoryEventPublisher eventPublisher = new InMemoryEventPublisher();
            
            // Create a mock AWS Secrets Manager provider
            MockSecretProvider awsProvider = MockSecretProvider.builder()
                    .withStoreType(StoreType.AWS_SECRETS_MANAGER)
                    .withDelayRange(50, 200) // Simulate network latency
                    .withFailureProbability(0.1) // 10% chance of simulated failure
                    .build();
            
            // Add a few example secrets
            awsProvider.addSecret("database/credentials", "{\"username\":\"dbuser\",\"password\":\"dbpass123\"}");
            awsProvider.addSecret("api/keys", "{\"api_key\":\"sk_test_12345abcdef\"}");
            
            // Create a mock CyberArk provider
            MockSecretProvider cyberArkProvider = MockSecretProvider.builder()
                    .withStoreType(StoreType.CYBERARK)
                    .withDelayRange(100, 300) // Slightly slower than AWS
                    .withFailureProbability(0.1)
                    .build();
            
            // Add example CyberArk secrets
            cyberArkProvider.addSecret("app1-credentials", "app1:secretpassword");
            
            // Set up a secret with active/inactive versions to demonstrate rollover
            cyberArkProvider.addSecret("rotating-secret", "active-value-1", "active");
            cyberArkProvider.addSecret("rotating-secret", "inactive-value-1", "inactive");
            
            // Create an in-memory cache
            InMemorySecretCache cache = new InMemorySecretCache();
            cache.setDefaultTTL(Duration.ofMinutes(5)); // Short TTL for demo purposes
            
            // Create a polling refresh policy that checks every 30 seconds
            MockPollingRefreshPolicy refreshPolicy = new MockPollingRefreshPolicy(
                    Duration.ofSeconds(30), 
                    eventPublisher::publish);
            
            // Subscribe to events for demonstration
            eventPublisher.subscribe(Object.class, event -> 
                    System.out.println("Event published: " + event));
            
            // Build the SecretManager with all components
            try (SecretManager secretManager = SecretManager.builder()
                    .withProvider(awsProvider)
                    .withProvider(cyberArkProvider)
                    .withCache(cache)
                    .withDefaultRefreshPolicy(refreshPolicy)
                    .withEventPublisher(eventPublisher)
                    .withDefaultCacheTTL(Duration.ofMinutes(5))
                    .build()) {
                
                // Register secrets with the manager
                
                // AWS secret using IAM role
                SecretReference awsDbRef = new SecretReference(
                        StoreType.AWS_SECRETS_MANAGER, 
                        "database/credentials", 
                        "latest");
                
                AccessCredential awsCredential = AccessCredential.forIamRole(
                        STSAssumeRoleConfig.builder()
                                .withRoleArn("arn:aws:iam::123456789012:role/SecretsAccessRole")
                                .withRoleSessionName("example-session")
                                .build());
                
                secretManager.registerSecret(
                        "db-creds", 
                        awsDbRef, 
                        awsCredential, 
                        refreshPolicy);
                
                // CyberArk secret using API key
                SecretReference cyberArkAppRef = new SecretReference(
                        StoreType.CYBERARK, 
                        "app1-credentials", 
                        "latest");
                
                AccessCredential cyberArkCredential = AccessCredential.forCyberArkApiKey(
                        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example");
                
                secretManager.registerSecret(
                        "app1-creds", 
                        cyberArkAppRef, 
                        cyberArkCredential, 
                        refreshPolicy);
                
                // Rotating secret
                SecretReference rotatingRef = new SecretReference(
                        StoreType.CYBERARK, 
                        "rotating-secret", 
                        "active");
                
                secretManager.registerSecret(
                        "rotating-secret", 
                        rotatingRef, 
                        cyberArkCredential, 
                        refreshPolicy);
                
                // Start the interactive demo
                runInteractiveDemo(secretManager, cyberArkProvider);
            }
            
        } catch (Exception e) {
            System.err.println("Error in example: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Runs an interactive demo allowing the user to try different operations.
     *
     * @param secretManager The SecretManager to use
     * @param mockProvider  The mock provider for simulating rollover
     */
    private static void runInteractiveDemo(
            SecretManager secretManager, 
            MockSecretProvider mockProvider) {
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        System.out.println("\n===== Java Secrets Library Demo =====\n");
        System.out.println("Available secrets: db-creds, app1-creds, rotating-secret");
        
        while (running) {
            System.out.println("\nOptions:");
            System.out.println("1. Get secret");
            System.out.println("2. Refresh secret");
            System.out.println("3. Simulate secret rollover");
            System.out.println("4. Exit");
            System.out.print("\nEnter choice (1-4): ");
            
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("Enter secret name: ");
                        String secretName = scanner.nextLine().trim();
                        
                        long startTime = System.currentTimeMillis();
                        Secret secret = secretManager.getSecret(secretName);
                        long endTime = System.currentTimeMillis();
                        
                        System.out.println("Secret retrieved in " + (endTime - startTime) + "ms");
                        System.out.println("Name: " + secret.getName());
                        System.out.println("Value: " + new String(secret.getValue()));
                        System.out.println("Version: " + secret.getMetadata().getVersion());
                        System.out.println("Last Retrieved: " + secret.getMetadata().getLastRetrieved());
                    }
                    case "2" -> {
                        System.out.print("Enter secret name: ");
                        String secretName = scanner.nextLine().trim();
                        
                        long startTime = System.currentTimeMillis();
                        Secret secret = secretManager.refreshSecret(secretName);
                        long endTime = System.currentTimeMillis();
                        
                        System.out.println("Secret refreshed in " + (endTime - startTime) + "ms");
                        System.out.println("Name: " + secret.getName());
                        System.out.println("Value: " + new String(secret.getValue()));
                        System.out.println("Version: " + secret.getMetadata().getVersion());
                        System.out.println("Last Retrieved: " + secret.getMetadata().getLastRetrieved());
                    }
                    case "3" -> {
                        System.out.println("Simulating rollover for 'rotating-secret'...");
                        boolean success = mockProvider.performRollover("rotating-secret");
                        if (success) {
                            System.out.println("Rollover successful. The active and inactive versions have been swapped.");
                            System.out.println("Try retrieving the secret again to see the new value.");
                        } else {
                            System.out.println("Rollover failed. Check if the secret has both active and inactive versions.");
                        }
                    }
                    case "4" -> {
                        running = false;
                        System.out.println("Exiting...");
                    }
                    default -> System.out.println("Invalid choice. Please enter a number between 1 and 4.");
                }
            } catch (SecretAccessException e) {
                System.err.println("Error accessing secret: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
}
