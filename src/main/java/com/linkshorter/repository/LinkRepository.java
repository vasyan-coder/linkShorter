package com.linkshorter.repository;

import com.linkshorter.model.Link;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for storing and managing links
 * Thread-safe implementation using ConcurrentHashMap
 */
public class LinkRepository {
    private final Map<String, Link> linksByShortCode;
    private final Map<UUID, Set<String>> linksByUser;

    public LinkRepository() {
        this.linksByShortCode = new ConcurrentHashMap<>();
        this.linksByUser = new ConcurrentHashMap<>();
    }

    /**
     * Save a new link
     */
    public void save(Link link) {
        if (link == null) {
            throw new IllegalArgumentException("Link cannot be null");
        }

        linksByShortCode.put(link.getShortCode(), link);

        linksByUser.computeIfAbsent(link.getOwnerId(), k -> ConcurrentHashMap.newKeySet())
                .add(link.getShortCode());
    }

    /**
     * Find a link by its short code
     */
    public Optional<Link> findByShortCode(String shortCode) {
        return Optional.ofNullable(linksByShortCode.get(shortCode));
    }

    /**
     * Find all links owned by a user
     */
    public List<Link> findByOwnerId(UUID ownerId) {
        Set<String> shortCodes = linksByUser.getOrDefault(ownerId, Collections.emptySet());
        return shortCodes.stream()
                .map(linksByShortCode::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Delete a link by short code
     */
    public boolean delete(String shortCode) {
        Link link = linksByShortCode.remove(shortCode);
        if (link != null) {
            Set<String> userLinks = linksByUser.get(link.getOwnerId());
            if (userLinks != null) {
                userLinks.remove(shortCode);
            }
            return true;
        }
        return false;
    }

    /**
     * Check if a short code already exists
     */
    public boolean exists(String shortCode) {
        return linksByShortCode.containsKey(shortCode);
    }

    /**
     * Get all links
     */
    public List<Link> findAll() {
        return new ArrayList<>(linksByShortCode.values());
    }

    /**
     * Get total number of links
     */
    public int count() {
        return linksByShortCode.size();
    }

    /**
     * Clear all links (useful for testing)
     */
    public void clear() {
        linksByShortCode.clear();
        linksByUser.clear();
    }
}

