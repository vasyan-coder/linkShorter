package com.linkshorter.service;

import com.linkshorter.config.AppConfiguration;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Scheduler for automatic cleanup of expired links
 */
public class CleanupScheduler {
    private final LinkService linkService;
    private final AppConfiguration config;
    private Timer timer;

    public CleanupScheduler(LinkService linkService, AppConfiguration config) {
        this.linkService = linkService;
        this.config = config;
    }

    /**
     * Start the cleanup scheduler
     */
    public void start() {
        if (timer != null) {
            return; // Already started
        }

        timer = new Timer("LinkCleanupScheduler", true);
        long interval = config.getCleanupInterval();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    int removed = linkService.cleanupExpiredLinks();
                    if (removed > 0) {
                        System.out.println("[Cleanup] Удалено истёкших ссылок: " + removed);
                    }
                } catch (Exception e) {
                    System.err.println("[Cleanup] Ошибка при очистке: " + e.getMessage());
                }
            }
        }, interval, interval);

        System.out.println("[Cleanup] Планировщик очистки запущен (интервал: " +
                (interval / 1000 / 60) + " минут)");
    }

    /**
     * Stop the cleanup scheduler
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            System.out.println("[Cleanup] Планировщик очистки остановлен");
        }
    }
}

