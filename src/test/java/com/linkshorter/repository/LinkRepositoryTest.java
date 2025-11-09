package com.linkshorter.repository;

import com.linkshorter.model.Link;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkRepositoryTest {

    private LinkRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LinkRepository();
    }

    @Test
    void testSaveAndFindByShortCode() {
        Link link = createTestLink("abc123", UUID.randomUUID());
        repository.save(link);

        Optional<Link> found = repository.findByShortCode("abc123");

        assertTrue(found.isPresent());
        assertEquals(link, found.get());
    }

    @Test
    void testSaveNullLink() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    void testFindByShortCodeNotFound() {
        Optional<Link> found = repository.findByShortCode("nonexistent");
        assertTrue(found.isEmpty());
    }

    @Test
    void testFindByOwnerId() {
        UUID ownerId = UUID.randomUUID();
        Link link1 = createTestLink("abc123", ownerId);
        Link link2 = createTestLink("def456", ownerId);
        Link link3 = createTestLink("ghi789", UUID.randomUUID());

        repository.save(link1);
        repository.save(link2);
        repository.save(link3);

        List<Link> ownerLinks = repository.findByOwnerId(ownerId);

        assertEquals(2, ownerLinks.size());
        assertTrue(ownerLinks.contains(link1));
        assertTrue(ownerLinks.contains(link2));
        assertFalse(ownerLinks.contains(link3));
    }

    @Test
    void testFindByOwnerIdNoLinks() {
        UUID ownerId = UUID.randomUUID();
        List<Link> links = repository.findByOwnerId(ownerId);

        assertTrue(links.isEmpty());
    }

    @Test
    void testDelete() {
        Link link = createTestLink("abc123", UUID.randomUUID());
        repository.save(link);

        assertTrue(repository.exists("abc123"));

        boolean deleted = repository.delete("abc123");

        assertTrue(deleted);
        assertFalse(repository.exists("abc123"));
        assertTrue(repository.findByShortCode("abc123").isEmpty());
    }

    @Test
    void testDeleteNonexistent() {
        boolean deleted = repository.delete("nonexistent");
        assertFalse(deleted);
    }

    @Test
    void testExists() {
        Link link = createTestLink("abc123", UUID.randomUUID());

        assertFalse(repository.exists("abc123"));

        repository.save(link);

        assertTrue(repository.exists("abc123"));
    }

    @Test
    void testFindAll() {
        Link link1 = createTestLink("abc123", UUID.randomUUID());
        Link link2 = createTestLink("def456", UUID.randomUUID());
        Link link3 = createTestLink("ghi789", UUID.randomUUID());

        repository.save(link1);
        repository.save(link2);
        repository.save(link3);

        List<Link> allLinks = repository.findAll();

        assertEquals(3, allLinks.size());
        assertTrue(allLinks.contains(link1));
        assertTrue(allLinks.contains(link2));
        assertTrue(allLinks.contains(link3));
    }

    @Test
    void testCount() {
        assertEquals(0, repository.count());

        repository.save(createTestLink("abc123", UUID.randomUUID()));
        assertEquals(1, repository.count());

        repository.save(createTestLink("def456", UUID.randomUUID()));
        assertEquals(2, repository.count());

        repository.delete("abc123");
        assertEquals(1, repository.count());
    }

    @Test
    void testClear() {
        repository.save(createTestLink("abc123", UUID.randomUUID()));
        repository.save(createTestLink("def456", UUID.randomUUID()));

        assertEquals(2, repository.count());

        repository.clear();

        assertEquals(0, repository.count());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void testUpdateLink() {
        UUID ownerId = UUID.randomUUID();
        Link link = createTestLink("abc123", ownerId);
        repository.save(link);

        // Create updated version
        Link updatedLink = new Link.Builder()
                .shortCode("abc123")
                .originalUrl("https://updated.com")
                .ownerId(ownerId)
                .expiresAt(Instant.now().plusSeconds(7200))
                .clickLimit(200)
                .build();

        repository.save(updatedLink);

        Optional<Link> found = repository.findByShortCode("abc123");
        assertTrue(found.isPresent());
        assertEquals("https://updated.com", found.get().getOriginalUrl());
        assertEquals(200, found.get().getClickLimit());
    }

    private Link createTestLink(String shortCode, UUID ownerId) {
        return new Link.Builder()
                .shortCode(shortCode)
                .originalUrl("https://example.com/" + shortCode)
                .ownerId(ownerId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .clickLimit(100)
                .build();
    }
}

