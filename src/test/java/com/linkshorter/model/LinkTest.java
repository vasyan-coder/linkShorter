package com.linkshorter.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkTest {

    @Test
    void testBuildValidLink() {
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(3600);

        Link link = new Link.Builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .ownerId(ownerId)
                .createdAt(now)
                .expiresAt(expires)
                .clickLimit(100)
                .build();

        assertEquals("abc123", link.getShortCode());
        assertEquals("https://example.com", link.getOriginalUrl());
        assertEquals(ownerId, link.getOwnerId());
        assertEquals(now, link.getCreatedAt());
        assertEquals(expires, link.getExpiresAt());
        assertEquals(100, link.getClickLimit());
        assertEquals(0, link.getClickCount());
        assertTrue(link.isActive());
    }

    @Test
    void testBuildLinkWithoutCreatedAt() {
        UUID ownerId = UUID.randomUUID();

        Link link = new Link.Builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .ownerId(ownerId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .clickLimit(100)
                .build();

        assertNotNull(link.getCreatedAt());
    }

    @Test
    void testBuildLinkWithNullShortCode() {
        assertThrows(IllegalArgumentException.class, () ->
                new Link.Builder()
                        .shortCode(null)
                        .originalUrl("https://example.com")
                        .ownerId(UUID.randomUUID())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .clickLimit(100)
                        .build()
        );
    }

    @Test
    void testBuildLinkWithEmptyShortCode() {
        assertThrows(IllegalArgumentException.class, () ->
                new Link.Builder()
                        .shortCode("")
                        .originalUrl("https://example.com")
                        .ownerId(UUID.randomUUID())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .clickLimit(100)
                        .build()
        );
    }

    @Test
    void testBuildLinkWithNullUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                new Link.Builder()
                        .shortCode("abc123")
                        .originalUrl(null)
                        .ownerId(UUID.randomUUID())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .clickLimit(100)
                        .build()
        );
    }

    @Test
    void testBuildLinkWithNullOwnerId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Link.Builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .ownerId(null)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .clickLimit(100)
                        .build()
        );
    }

    @Test
    void testBuildLinkWithNullExpiresAt() {
        assertThrows(IllegalArgumentException.class, () ->
                new Link.Builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .ownerId(UUID.randomUUID())
                        .expiresAt(null)
                        .clickLimit(100)
                        .build()
        );
    }

    @Test
    void testBuildLinkWithInvalidClickLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                new Link.Builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .ownerId(UUID.randomUUID())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .clickLimit(0)
                        .build()
        );
    }

    @Test
    void testIncrementClickCount() {
        Link link = createTestLink(10);

        assertEquals(0, link.getClickCount());

        link.incrementClickCount();
        assertEquals(1, link.getClickCount());

        link.incrementClickCount();
        assertEquals(2, link.getClickCount());
    }

    @Test
    void testClickLimitReached() {
        Link link = createTestLink(3);

        assertFalse(link.hasReachedClickLimit());
        assertTrue(link.isActive());

        link.incrementClickCount();
        link.incrementClickCount();
        assertFalse(link.hasReachedClickLimit());

        link.incrementClickCount();
        assertTrue(link.hasReachedClickLimit());
        assertFalse(link.isActive());
    }

    @Test
    void testIsExpired() {
        Instant past = Instant.now().minusSeconds(3600);

        Link link = new Link.Builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .ownerId(UUID.randomUUID())
                .expiresAt(past)
                .clickLimit(100)
                .build();

        assertTrue(link.isExpired());
    }

    @Test
    void testIsNotExpired() {
        Link link = createTestLink(100);
        assertFalse(link.isExpired());
    }

    @Test
    void testIsOwnedBy() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Link link = new Link.Builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .ownerId(ownerId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .clickLimit(100)
                .build();

        assertTrue(link.isOwnedBy(ownerId));
        assertFalse(link.isOwnedBy(otherId));
    }

    @Test
    void testGetRemainingClicks() {
        Link link = createTestLink(10);

        assertEquals(10, link.getRemainingClicks());

        link.incrementClickCount();
        assertEquals(9, link.getRemainingClicks());

        link.incrementClickCount();
        link.incrementClickCount();
        assertEquals(7, link.getRemainingClicks());
    }

    @Test
    void testDeactivate() {
        Link link = createTestLink(10);

        assertTrue(link.isActive());
        link.deactivate();
        assertFalse(link.isActive());
    }

    @Test
    void testEquality() {
        Link link1 = createTestLink(10);
        Link link2 = new Link.Builder()
                .shortCode(link1.getShortCode())
                .originalUrl("https://different.com")
                .ownerId(UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(7200))
                .clickLimit(50)
                .build();

        assertEquals(link1, link2);
        assertEquals(link1.hashCode(), link2.hashCode());
    }

    private Link createTestLink(int clickLimit) {
        return new Link.Builder()
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .ownerId(UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(3600))
                .clickLimit(clickLimit)
                .build();
    }
}

