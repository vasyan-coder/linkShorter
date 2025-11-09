package com.linkshorter.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user in the system
 * Users are identified by UUID without authentication
 */
public class User {
    private final UUID id;

    public User(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        this.id = id;
    }

    public static User createNew() {
        return new User(UUID.randomUUID());
    }

    public static User fromId(String idString) {
        try {
            return new User(UUID.fromString(idString));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format: " + idString);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getIdString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{id=" + id + "}";
    }
}

