package com.linkshorter.integration;

import com.linkshorter.config.AppConfiguration;
import com.linkshorter.model.Link;
import com.linkshorter.model.User;
import com.linkshorter.repository.LinkRepository;
import com.linkshorter.service.LinkService;
import com.linkshorter.service.NotificationService;
import com.linkshorter.service.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete link shortening workflow
 */
class LinkServiceIntegrationTest {

    private LinkService linkService;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        LinkRepository repository = new LinkRepository();
        AppConfiguration config = new AppConfiguration();
        ShortCodeGenerator codeGenerator = new ShortCodeGenerator(config.getShortCodeLength());
        NotificationService notificationService = new NotificationService(false);

        linkService = new LinkService(repository, codeGenerator, notificationService, config);

        user1 = User.createNew();
        user2 = User.createNew();
    }

    @Test
    void testCompleteUserWorkflow() {
        // User creates a link
        Link link = linkService.createLink("https://www.baeldung.com/java-9-http-client", user1, 5);
        String shortCode = link.getShortCode();

        assertNotNull(shortCode);
        assertEquals(6, shortCode.length());

        // User lists their links
        List<Link> links = linkService.getUserLinks(user1);
        assertEquals(1, links.size());

        // User follows the link 3 times
        for (int i = 0; i < 3; i++) {
            Optional<String> url = linkService.followLink(shortCode);
            assertTrue(url.isPresent());
            assertEquals("https://www.baeldung.com/java-9-http-client", url.get());
        }

        // Check click count
        Optional<Link> updatedLink = linkService.getLink(shortCode);
        assertTrue(updatedLink.isPresent());
        assertEquals(3, updatedLink.get().getClickCount());

        // User updates click limit
        boolean updated = linkService.updateClickLimit(shortCode, user1, 10);
        assertTrue(updated);

        // User deletes the link
        boolean deleted = linkService.deleteLink(shortCode, user1);
        assertTrue(deleted);

        // Link should no longer exist
        Optional<Link> deletedLink = linkService.getLink(shortCode);
        assertTrue(deletedLink.isEmpty());
    }

    @Test
    void testMultiUserScenario() {
        // Both users create links for the same URL
        String url = "https://example.com/article";
        Link link1 = linkService.createLink(url, user1, 10);
        Link link2 = linkService.createLink(url, user2, 20);

        // Should have different short codes
        assertNotEquals(link1.getShortCode(), link2.getShortCode());

        // Each user should see only their own links
        List<Link> user1Links = linkService.getUserLinks(user1);
        List<Link> user2Links = linkService.getUserLinks(user2);

        assertEquals(1, user1Links.size());
        assertEquals(1, user2Links.size());

        assertTrue(user1Links.get(0).isOwnedBy(user1.getId()));
        assertTrue(user2Links.get(0).isOwnedBy(user2.getId()));

        // User1 cannot delete User2's link
        boolean deletedByWrongUser = linkService.deleteLink(link2.getShortCode(), user1);
        assertFalse(deletedByWrongUser);

        // User2 can delete their own link
        boolean deletedByOwner = linkService.deleteLink(link2.getShortCode(), user2);
        assertTrue(deletedByOwner);
    }

    @Test
    void testClickLimitExhaustion() {
        Link link = linkService.createLink("https://example.com", user1, 3);
        String shortCode = link.getShortCode();

        // Follow link 3 times (the limit)
        for (int i = 0; i < 3; i++) {
            Optional<String> url = linkService.followLink(shortCode);
            assertTrue(url.isPresent(), "Click " + (i + 1) + " should succeed");
        }

        // Fourth click should fail
        Optional<String> url = linkService.followLink(shortCode);
        assertTrue(url.isEmpty(), "Click after limit should fail");

        // Link should be inactive
        Optional<Link> link4 = linkService.getLink(shortCode);
        assertTrue(link4.isPresent());
        assertFalse(link4.get().isActive());
    }

    @Test
    void testMultipleLinksPerUser() {
        // User creates multiple links
        Link link1 = linkService.createLink("https://example1.com", user1);
        Link link2 = linkService.createLink("https://example2.com", user1);
        Link link3 = linkService.createLink("https://example3.com", user1);

        // All should have unique short codes
        assertNotEquals(link1.getShortCode(), link2.getShortCode());
        assertNotEquals(link2.getShortCode(), link3.getShortCode());
        assertNotEquals(link1.getShortCode(), link3.getShortCode());

        // User should see all their links
        List<Link> userLinks = linkService.getUserLinks(user1);
        assertEquals(3, userLinks.size());

        // Follow different links
        Optional<String> url1 = linkService.followLink(link1.getShortCode());
        Optional<String> url2 = linkService.followLink(link2.getShortCode());
        Optional<String> url3 = linkService.followLink(link3.getShortCode());

        assertTrue(url1.isPresent());
        assertTrue(url2.isPresent());
        assertTrue(url3.isPresent());

        assertEquals("https://example1.com", url1.get());
        assertEquals("https://example2.com", url2.get());
        assertEquals("https://example3.com", url3.get());
    }

    @Test
    void testLinkManagement() {
        // Create several links
        Link link1 = linkService.createLink("https://example1.com", user1);
        Link link2 = linkService.createLink("https://example2.com", user1);
        Link link3 = linkService.createLink("https://example3.com", user1);

        // Update one link
        linkService.updateClickLimit(link2.getShortCode(), user1, 50);

        // Delete one link
        linkService.deleteLink(link1.getShortCode(), user1);

        // Verify state
        List<Link> remainingLinks = linkService.getUserLinks(user1);
        assertEquals(2, remainingLinks.size());

        Optional<Link> updatedLink = linkService.getLink(link2.getShortCode());
        assertTrue(updatedLink.isPresent());
        assertEquals(50, updatedLink.get().getClickLimit());
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        Link link = linkService.createLink("https://example.com", user1, 100);
        String shortCode = link.getShortCode();

        // Simulate concurrent clicks
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    linkService.followLink(shortCode);
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Should have 50 clicks total
        Optional<Link> finalLink = linkService.getLink(shortCode);
        assertTrue(finalLink.isPresent());
        assertEquals(50, finalLink.get().getClickCount());
    }
}

