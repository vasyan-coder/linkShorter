package com.linkshorter.service;

import com.linkshorter.config.AppConfiguration;
import com.linkshorter.model.Link;
import com.linkshorter.model.User;
import com.linkshorter.repository.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {

    private LinkService linkService;
    private LinkRepository repository;
    private User user;

    @BeforeEach
    void setUp() {
        repository = new LinkRepository();
        AppConfiguration config = new AppConfiguration();
        ShortCodeGenerator codeGenerator = new ShortCodeGenerator(config.getShortCodeLength());
        NotificationService notificationService = new NotificationService(false); // Disable for tests

        linkService = new LinkService(repository, codeGenerator, notificationService, config);
        user = User.createNew();
    }

    @Test
    void testCreateLink() {
        String url = "https://example.com";
        Link link = linkService.createLink(url, user);

        assertNotNull(link);
        assertEquals(url, link.getOriginalUrl());
        assertEquals(user.getId(), link.getOwnerId());
        assertTrue(link.isActive());
        assertNotNull(link.getShortCode());
    }

    @Test
    void testCreateLinkWithCustomClickLimit() {
        String url = "https://example.com";
        Link link = linkService.createLink(url, user, 50);

        assertEquals(50, link.getClickLimit());
    }

    @Test
    void testCreateLinkWithInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                linkService.createLink("not-a-url", user)
        );
    }

    @Test
    void testCreateLinkWithNullUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                linkService.createLink(null, user)
        );
    }

    @Test
    void testCreateLinkWithEmptyUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                linkService.createLink("", user)
        );
    }

    @Test
    void testCreateLinkWithInvalidClickLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                linkService.createLink("https://example.com", user, 0)
        );
    }

    @Test
    void testFollowLink() {
        Link link = linkService.createLink("https://example.com", user);
        String shortCode = link.getShortCode();

        Optional<String> url = linkService.followLink(shortCode);

        assertTrue(url.isPresent());
        assertEquals("https://example.com", url.get());
    }

    @Test
    void testFollowLinkIncrementsClickCount() {
        Link link = linkService.createLink("https://example.com", user, 10);
        String shortCode = link.getShortCode();

        assertEquals(0, link.getClickCount());

        linkService.followLink(shortCode);
        Optional<Link> updated = linkService.getLink(shortCode);

        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getClickCount());
    }

    @Test
    void testFollowLinkExceedsLimit() {
        Link link = linkService.createLink("https://example.com", user, 2);
        String shortCode = link.getShortCode();

        // First click
        Optional<String> url1 = linkService.followLink(shortCode);
        assertTrue(url1.isPresent());

        // Second click
        Optional<String> url2 = linkService.followLink(shortCode);
        assertTrue(url2.isPresent());

        // Third click - should fail
        Optional<String> url3 = linkService.followLink(shortCode);
        assertTrue(url3.isEmpty());
    }

    @Test
    void testFollowNonexistentLink() {
        Optional<String> url = linkService.followLink("nonexistent");
        assertTrue(url.isEmpty());
    }

    @Test
    void testGetLink() {
        Link link = linkService.createLink("https://example.com", user);
        String shortCode = link.getShortCode();

        Optional<Link> found = linkService.getLink(shortCode);

        assertTrue(found.isPresent());
        assertEquals(link.getShortCode(), found.get().getShortCode());
    }

    @Test
    void testGetUserLinks() {
        linkService.createLink("https://example1.com", user);
        linkService.createLink("https://example2.com", user);
        linkService.createLink("https://example3.com", user);

        List<Link> userLinks = linkService.getUserLinks(user);

        assertEquals(3, userLinks.size());
    }

    @Test
    void testGetUserLinksIsolation() {
        User user2 = User.createNew();

        linkService.createLink("https://example1.com", user);
        linkService.createLink("https://example2.com", user2);

        List<Link> user1Links = linkService.getUserLinks(user);
        List<Link> user2Links = linkService.getUserLinks(user2);

        assertEquals(1, user1Links.size());
        assertEquals(1, user2Links.size());
    }

    @Test
    void testDeleteLink() {
        Link link = linkService.createLink("https://example.com", user);
        String shortCode = link.getShortCode();

        boolean deleted = linkService.deleteLink(shortCode, user);

        assertTrue(deleted);
        assertTrue(linkService.getLink(shortCode).isEmpty());
    }

    @Test
    void testDeleteLinkAccessControl() {
        Link link = linkService.createLink("https://example.com", user);
        String shortCode = link.getShortCode();

        User otherUser = User.createNew();
        boolean deleted = linkService.deleteLink(shortCode, otherUser);

        assertFalse(deleted);
        assertTrue(linkService.getLink(shortCode).isPresent());
    }

    @Test
    void testUpdateClickLimit() {
        Link link = linkService.createLink("https://example.com", user, 10);
        String shortCode = link.getShortCode();

        boolean updated = linkService.updateClickLimit(shortCode, user, 20);

        assertTrue(updated);

        Optional<Link> updatedLink = linkService.getLink(shortCode);
        assertTrue(updatedLink.isPresent());
        assertEquals(20, updatedLink.get().getClickLimit());
    }

    @Test
    void testUpdateClickLimitAccessControl() {
        Link link = linkService.createLink("https://example.com", user, 10);
        String shortCode = link.getShortCode();

        User otherUser = User.createNew();
        boolean updated = linkService.updateClickLimit(shortCode, otherUser, 20);

        assertFalse(updated);

        Optional<Link> unchangedLink = linkService.getLink(shortCode);
        assertTrue(unchangedLink.isPresent());
        assertEquals(10, unchangedLink.get().getClickLimit());
    }

    @Test
    void testUpdateClickLimitInvalid() {
        Link link = linkService.createLink("https://example.com", user);
        String shortCode = link.getShortCode();

        assertThrows(IllegalArgumentException.class, () ->
                linkService.updateClickLimit(shortCode, user, 0)
        );
    }

    @Test
    void testCleanupExpiredLinks() {
        // Create a link that's already expired
        Link link = new Link.Builder()
                .shortCode("expired")
                .originalUrl("https://example.com")
                .ownerId(user.getId())
                .expiresAt(Instant.now().minusSeconds(3600))
                .clickLimit(100)
                .build();

        repository.save(link);

        // Create a valid link
        linkService.createLink("https://valid.com", user);

        assertEquals(2, repository.count());

        int removed = linkService.cleanupExpiredLinks();

        assertEquals(1, removed);
        assertEquals(1, repository.count());
    }

    @Test
    void testDifferentUsersGetDifferentShortCodes() {
        User user1 = User.createNew();
        User user2 = User.createNew();

        String url = "https://example.com";
        Link link1 = linkService.createLink(url, user1);
        Link link2 = linkService.createLink(url, user2);

        assertNotEquals(link1.getShortCode(), link2.getShortCode());
    }

    @Test
    void testUrlValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                linkService.createLink("ftp://example.com", user)
        );

        assertThrows(IllegalArgumentException.class, () ->
                linkService.createLink("example.com", user)
        );
    }
}

