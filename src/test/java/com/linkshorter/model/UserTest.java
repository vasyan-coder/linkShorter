package com.linkshorter.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testCreateNewUser() {
        User user = User.createNew();

        assertNotNull(user);
        assertNotNull(user.getId());
        assertNotNull(user.getIdString());
    }

    @Test
    void testCreateUserWithId() {
        UUID id = UUID.randomUUID();
        User user = new User(id);

        assertEquals(id, user.getId());
        assertEquals(id.toString(), user.getIdString());
    }

    @Test
    void testCreateUserWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> new User(null));
    }

    @Test
    void testFromIdString() {
        UUID id = UUID.randomUUID();
        User user = User.fromId(id.toString());

        assertEquals(id, user.getId());
    }

    @Test
    void testFromInvalidIdString() {
        assertThrows(IllegalArgumentException.class, () -> User.fromId("invalid-uuid"));
    }

    @Test
    void testUserEquality() {
        UUID id = UUID.randomUUID();
        User user1 = new User(id);
        User user2 = new User(id);

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void testUserInequality() {
        User user1 = User.createNew();
        User user2 = User.createNew();

        assertNotEquals(user1, user2);
    }

    @Test
    void testToString() {
        User user = User.createNew();
        String str = user.toString();

        assertTrue(str.contains("User"));
        assertTrue(str.contains(user.getId().toString()));
    }
}

