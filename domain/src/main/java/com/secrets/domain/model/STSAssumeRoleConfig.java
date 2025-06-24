package com.secrets.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing configuration for AWS STS AssumeRole operation.
 * This is used when accessing secrets with an IAM role.
 */
public final class STSAssumeRoleConfig {
    private final String roleArn;
    private final String sessionName;
    private final Integer durationSeconds;
    private final String externalId;

    /**
     * Builder for creating STSAssumeRoleConfig instances.
     */
    public static class Builder {
        private String roleArn;
        private String sessionName = "SecretAccessSession";
        private Integer durationSeconds = 900; // 15 minutes default
        private String externalId;

        public Builder(String roleArn) {
            this.roleArn = roleArn;
        }

        public Builder withSessionName(String sessionName) {
            this.sessionName = sessionName;
            return this;
        }

        public Builder withDurationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder withExternalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public STSAssumeRoleConfig build() {
            return new STSAssumeRoleConfig(roleArn, sessionName, durationSeconds, externalId);
        }
    }

    private STSAssumeRoleConfig(String roleArn, String sessionName, Integer durationSeconds, String externalId) {
        this.roleArn = Objects.requireNonNull(roleArn, "Role ARN cannot be null");
        this.sessionName = Objects.requireNonNull(sessionName, "Session name cannot be null");
        this.durationSeconds = Objects.requireNonNull(durationSeconds, "Duration seconds cannot be null");
        this.externalId = externalId; // External ID is optional
    }

    public static Builder builder(String roleArn) {
        return new Builder(roleArn);
    }

    public String getRoleArn() {
        return roleArn;
    }

    public String getSessionName() {
        return sessionName;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public Optional<String> getExternalId() {
        return Optional.ofNullable(externalId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        STSAssumeRoleConfig that = (STSAssumeRoleConfig) o;
        return Objects.equals(roleArn, that.roleArn) &&
                Objects.equals(sessionName, that.sessionName) &&
                Objects.equals(durationSeconds, that.durationSeconds) &&
                Objects.equals(externalId, that.externalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleArn, sessionName, durationSeconds, externalId);
    }

    @Override
    public String toString() {
        return "STSAssumeRoleConfig{" +
                "roleArn='" + roleArn + '\'' +
                ", sessionName='" + sessionName + '\'' +
                ", durationSeconds=" + durationSeconds +
                ", externalId='" + (externalId != null ? "[REDACTED]" : null) + '\'' +
                '}';
    }
}
