package com.linkshorter.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a shortened link with its metadata
 */
public class Link {
    private final String shortCode;
    private final String originalUrl;
    private final UUID ownerId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final int clickLimit;
    private int clickCount;
    private boolean active;

    private Link(Builder builder) {
        this.shortCode = builder.shortCode;
        this.originalUrl = builder.originalUrl;
        this.ownerId = builder.ownerId;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
        this.clickLimit = builder.clickLimit;
        this.clickCount = 0;
        this.active = true;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getClickLimit() {
        return clickLimit;
    }

    public int getClickCount() {
        return clickCount;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean hasReachedClickLimit() {
        return clickCount >= clickLimit;
    }

    public synchronized void incrementClickCount() {
        if (active && !isExpired() && !hasReachedClickLimit()) {
            this.clickCount++;
            if (hasReachedClickLimit()) {
                deactivate();
            }
        }
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerId.equals(userId);
    }

    public int getRemainingClicks() {
        return Math.max(0, clickLimit - clickCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(shortCode, link.shortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortCode);
    }

    @Override
    public String toString() {
        return "Link{" +
                "shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", ownerId=" + ownerId +
                ", clickCount=" + clickCount +
                ", clickLimit=" + clickLimit +
                ", active=" + active +
                ", expiresAt=" + expiresAt +
                '}';
    }

    public static class Builder {
        private String shortCode;
        private String originalUrl;
        private UUID ownerId;
        private Instant createdAt;
        private Instant expiresAt;
        private int clickLimit;

        public Builder shortCode(String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public Builder originalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        public Builder ownerId(UUID ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder clickLimit(int clickLimit) {
            this.clickLimit = clickLimit;
            return this;
        }

        public Link build() {
            validateFields();
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            return new Link(this);
        }

        private void validateFields() {
            if (shortCode == null || shortCode.isBlank()) {
                throw new IllegalArgumentException("Short code cannot be null or empty");
            }
            if (originalUrl == null || originalUrl.isBlank()) {
                throw new IllegalArgumentException("Original URL cannot be null or empty");
            }
            if (ownerId == null) {
                throw new IllegalArgumentException("Owner ID cannot be null");
            }
            if (expiresAt == null) {
                throw new IllegalArgumentException("Expiration time cannot be null");
            }
            if (clickLimit <= 0) {
                throw new IllegalArgumentException("Click limit must be positive");
            }
        }
    }
}

