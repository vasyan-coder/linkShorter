package com.linkshorter.service;

import com.linkshorter.model.Link;

import java.util.UUID;

/**
 * Service for sending notifications to users
 */
public class NotificationService {
    private final boolean enabled;

    public NotificationService(boolean enabled) {
        this.enabled = enabled;
    }

    public void notifyLinkExpired(Link link) {
        if (!enabled) {
            return;
        }

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    УВЕДОМЛЕНИЕ                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Ссылка истекла по времени жизни (TTL)                  ║");
        System.out.println("║  Короткий код: " + String.format("%-39s", link.getShortCode()) + "║");
        System.out.println("║  Исходный URL: " + truncate(link.getOriginalUrl(), 38) + " ║");
        System.out.println("║                                                          ║");
        System.out.println("║  Создайте новую ссылку, если хотите продолжить           ║");
        System.out.println("║  использование данного URL.                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    public void notifyClickLimitReached(Link link) {
        if (!enabled) {
            return;
        }

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    УВЕДОМЛЕНИЕ                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Достигнут лимит переходов по ссылке                     ║");
        System.out.println("║  Короткий код: " + String.format("%-39s", link.getShortCode()) + "║");
        System.out.println("║  Исходный URL: " + truncate(link.getOriginalUrl(), 38) + " ║");
        System.out.println("║  Количество переходов: " + String.format("%-33s", link.getClickCount() + "/" + link.getClickLimit()) + "║");
        System.out.println("║                                                          ║");
        System.out.println("║  Создайте новую ссылку, если хотите продолжить           ║");
        System.out.println("║  использование данного URL.                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    public void notifyLinkCreated(String shortCode, String fullShortUrl, int clickLimit, long ttlHours) {
        if (!enabled) {
            return;
        }

        System.out.println("\n✓ Короткая ссылка успешно создана!");
        System.out.println("  Короткий код: " + shortCode);
        System.out.println("  Полная короткая ссылка: " + fullShortUrl);
        System.out.println("  Лимит переходов: " + clickLimit);
        System.out.println("  Время жизни: " + ttlHours + " часов");
    }

    public void notifyLinkNotFound(String shortCode) {
        if (!enabled) {
            return;
        }

        System.out.println("\n✗ Ошибка: Ссылка с кодом '" + shortCode + "' не найдена.");
    }

    public void notifyLinkInactive(Link link, String reason) {
        if (!enabled) {
            return;
        }

        System.out.println("\n✗ Ссылка недоступна: " + reason);
        System.out.println("  Короткий код: " + link.getShortCode());
        System.out.println("  Создайте новую ссылку для продолжения работы.");
    }

    public void notifyAccessDenied(String shortCode, UUID userId) {
        if (!enabled) {
            return;
        }

        System.out.println("\n✗ Доступ запрещён: Вы не являетесь владельцем ссылки '" + shortCode + "'");
        System.out.println("  Только владелец может редактировать или удалять свои ссылки.");
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return String.format("%-" + maxLength + "s", text);
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}

