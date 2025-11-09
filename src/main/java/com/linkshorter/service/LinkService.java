package com.linkshorter.service;

import com.linkshorter.config.AppConfiguration;
import com.linkshorter.model.Link;
import com.linkshorter.model.User;
import com.linkshorter.repository.LinkRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Main service for managing links
 * Handles creation, retrieval, and deletion of shortened links
 */
public class LinkService {
    private final LinkRepository repository;
    private final ShortCodeGenerator codeGenerator;
    private final NotificationService notificationService;
    private final AppConfiguration config;

    public LinkService(LinkRepository repository,
                       ShortCodeGenerator codeGenerator,
                       NotificationService notificationService,
                       AppConfiguration config) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.notificationService = notificationService;
        this.config = config;
    }

    /**
     * Create a new shortened link
     */
    public Link createLink(String originalUrl, User owner) {
        return createLink(originalUrl, owner, config.getDefaultClickLimit());
    }

    /**
     * Create a new shortened link with custom click limit
     */
    public Link createLink(String originalUrl, User owner, int clickLimit) {
        validateUrl(originalUrl);

        if (clickLimit <= 0) {
            throw new IllegalArgumentException("Click limit must be positive");
        }

        String shortCode = codeGenerator.generateShortCode(originalUrl, owner.getId());
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(config.getDefaultTtl());

        Link link = new Link.Builder()
                .shortCode(shortCode)
                .originalUrl(originalUrl)
                .ownerId(owner.getId())
                .createdAt(now)
                .expiresAt(expiresAt)
                .clickLimit(clickLimit)
                .build();

        repository.save(link);

        long ttlHours = config.getDefaultTtl() / (1000 * 60 * 60);
        notificationService.notifyLinkCreated(
                shortCode,
                config.getLinkDomain() + "/" + shortCode,
                clickLimit,
                ttlHours
        );

        return link;
    }

    /**
     * Get original URL and register a click
     */
    public Optional<String> followLink(String shortCode) {
        Optional<Link> linkOpt = repository.findByShortCode(shortCode);

        if (linkOpt.isEmpty()) {
            notificationService.notifyLinkNotFound(shortCode);
            return Optional.empty();
        }

        Link link = linkOpt.get();

        // Check if link is expired
        if (link.isExpired()) {
            link.deactivate();
            notificationService.notifyLinkExpired(link);
            repository.delete(shortCode);
            return Optional.empty();
        }

        // Check if link is active
        if (!link.isActive()) {
            notificationService.notifyLinkInactive(link, "Ссылка деактивирована");
            return Optional.empty();
        }

        // Check if click limit reached
        if (link.hasReachedClickLimit()) {
            link.deactivate();
            notificationService.notifyClickLimitReached(link);
            return Optional.empty();
        }

        // Increment click count
        link.incrementClickCount();

        // Check if limit just reached
        if (link.hasReachedClickLimit()) {
            notificationService.notifyClickLimitReached(link);
        }

        return Optional.of(link.getOriginalUrl());
    }

    /**
     * Get link information by short code
     */
    public Optional<Link> getLink(String shortCode) {
        return repository.findByShortCode(shortCode);
    }

    /**
     * Get all links for a user
     */
    public List<Link> getUserLinks(User user) {
        return repository.findByOwnerId(user.getId());
    }

    /**
     * Delete a link (only owner can delete)
     */
    public boolean deleteLink(String shortCode, User user) {
        Optional<Link> linkOpt = repository.findByShortCode(shortCode);

        if (linkOpt.isEmpty()) {
            notificationService.notifyLinkNotFound(shortCode);
            return false;
        }

        Link link = linkOpt.get();

        if (!link.isOwnedBy(user.getId())) {
            notificationService.notifyAccessDenied(shortCode, user.getId());
            return false;
        }

        return repository.delete(shortCode);
    }

    /**
     * Update click limit for a link (only owner can update)
     */
    public boolean updateClickLimit(String shortCode, User user, int newClickLimit) {
        if (newClickLimit <= 0) {
            throw new IllegalArgumentException("Click limit must be positive");
        }

        Optional<Link> linkOpt = repository.findByShortCode(shortCode);

        if (linkOpt.isEmpty()) {
            notificationService.notifyLinkNotFound(shortCode);
            return false;
        }

        Link link = linkOpt.get();

        if (!link.isOwnedBy(user.getId())) {
            notificationService.notifyAccessDenied(shortCode, user.getId());
            return false;
        }

        // Create updated link
        Link updatedLink = new Link.Builder()
                .shortCode(link.getShortCode())
                .originalUrl(link.getOriginalUrl())
                .ownerId(link.getOwnerId())
                .createdAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .clickLimit(newClickLimit)
                .build();

        repository.save(updatedLink);
        return true;
    }

    /**
     * Clean up expired links
     */
    public int cleanupExpiredLinks() {
        List<Link> allLinks = repository.findAll();
        int removedCount = 0;

        for (Link link : allLinks) {
            if (link.isExpired()) {
                repository.delete(link.getShortCode());
                removedCount++;
            }
        }

        return removedCount;
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
    }
}

