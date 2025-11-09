package com.linkshorter.cli;

import com.linkshorter.model.Link;
import com.linkshorter.model.User;
import com.linkshorter.service.LinkService;

import java.awt.*;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Processes CLI commands
 */
public class CommandProcessor {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private final LinkService linkService;
    private User currentUser;

    public CommandProcessor(LinkService linkService) {
        this.linkService = linkService;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Process a command
     */
    public void processCommand(String command) {
        if (command == null || command.isBlank()) {
            return;
        }

        String[] parts = command.trim().split("\\s+", 2);
        String action = parts[0].toLowerCase();

        try {
            switch (action) {
                case "create" -> handleCreate(parts);
                case "open" -> handleOpen(parts);
                case "list" -> handleList();
                case "info" -> handleInfo(parts);
                case "delete" -> handleDelete(parts);
                case "update" -> handleUpdate(parts);
                case "user" -> handleUser();
                case "help" -> handleHelp();
                case "exit" -> handleExit();
                default -> System.out.println("Неизвестная команда. Введите 'help' для справки.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Ошибка: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCreate(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Использование: create <URL> [лимит_переходов]");
            return;
        }

        String[] args = parts[1].split("\\s+");
        String url = args[0];
        int clickLimit = -1;

        if (args.length > 1) {
            try {
                clickLimit = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("✗ Неверный формат лимита переходов");
                return;
            }
        }

        Link link;
        if (clickLimit > 0) {
            link = linkService.createLink(url, currentUser, clickLimit);
        } else {
            link = linkService.createLink(url, currentUser);
        }
    }

    private void handleOpen(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Использование: open <короткий_код>");
            return;
        }

        String shortCode = parts[1].trim();
        Optional<String> urlOpt = linkService.followLink(shortCode);

        if (urlOpt.isPresent()) {
            String url = urlOpt.get();
            System.out.println("Переход по ссылке: " + url);

            // Open in browser
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                    System.out.println("✓ Ссылка открыта в браузере");
                } else {
                    System.out.println("✗ Открытие браузера не поддерживается на этой системе");
                    System.out.println("  Откройте вручную: " + url);
                }
            } catch (Exception e) {
                System.out.println("✗ Ошибка при открытии браузера: " + e.getMessage());
                System.out.println("  Откройте вручную: " + url);
            }
        }
    }

    private void handleList() {
        List<Link> links = linkService.getUserLinks(currentUser);

        if (links.isEmpty()) {
            System.out.println("\nУ вас пока нет сокращённых ссылок.");
            System.out.println("Создайте новую ссылку командой: create <URL>");
            return;
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("Ваши ссылки:");
        System.out.println("=".repeat(100));

        for (Link link : links) {
            printLinkInfo(link);
            System.out.println("-".repeat(100));
        }

        System.out.println("Всего ссылок: " + links.size());
    }

    private void handleInfo(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Использование: info <короткий_код>");
            return;
        }

        String shortCode = parts[1].trim();
        Optional<Link> linkOpt = linkService.getLink(shortCode);

        if (linkOpt.isEmpty()) {
            System.out.println("✗ Ссылка не найдена: " + shortCode);
            return;
        }

        Link link = linkOpt.get();
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Информация о ссылке:");
        System.out.println("=".repeat(80));
        printLinkInfo(link);
    }

    private void handleDelete(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Использование: delete <короткий_код>");
            return;
        }

        String shortCode = parts[1].trim();
        boolean deleted = linkService.deleteLink(shortCode, currentUser);

        if (deleted) {
            System.out.println("✓ Ссылка успешно удалена: " + shortCode);
        }
    }

    private void handleUpdate(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Использование: update <короткий_код> <новый_лимит>");
            return;
        }

        String[] args = parts[1].split("\\s+");
        if (args.length < 2) {
            System.out.println("Использование: update <короткий_код> <новый_лимит>");
            return;
        }

        String shortCode = args[0];
        int newLimit;

        try {
            newLimit = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("✗ Неверный формат лимита");
            return;
        }

        boolean updated = linkService.updateClickLimit(shortCode, currentUser, newLimit);

        if (updated) {
            System.out.println("✓ Лимит переходов обновлён для ссылки: " + shortCode);
            System.out.println("  Новый лимит: " + newLimit);
        }
    }

    private void handleUser() {
        System.out.println("\nТекущий пользователь:");
        System.out.println("  UUID: " + currentUser.getIdString());
        System.out.println("\nСохраните этот UUID для доступа к вашим ссылкам в будущем.");
    }

    private void handleHelp() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Доступные команды:");
        System.out.println("=".repeat(80));
        System.out.println("  create <URL> [лимит]    - Создать короткую ссылку");
        System.out.println("                            Пример: create https://example.com 50");
        System.out.println();
        System.out.println("  open <код>              - Открыть ссылку в браузере");
        System.out.println("                            Пример: open aBc123");
        System.out.println();
        System.out.println("  list                    - Показать все ваши ссылки");
        System.out.println();
        System.out.println("  info <код>              - Показать информацию о ссылке");
        System.out.println("                            Пример: info aBc123");
        System.out.println();
        System.out.println("  delete <код>            - Удалить ссылку");
        System.out.println("                            Пример: delete aBc123");
        System.out.println();
        System.out.println("  update <код> <лимит>    - Обновить лимит переходов");
        System.out.println("                            Пример: update aBc123 100");
        System.out.println();
        System.out.println("  user                    - Показать информацию о текущем пользователе");
        System.out.println();
        System.out.println("  help                    - Показать эту справку");
        System.out.println();
        System.out.println("  exit                    - Выйти из программы");
        System.out.println("=".repeat(80));
    }

    private void handleExit() {
        System.out.println("\nДо свидания!");
        System.exit(0);
    }

    private void printLinkInfo(Link link) {
        String status = link.isActive() ? "✓ Активна" : "✗ Неактивна";
        String expired = link.isExpired() ? " (ИСТЕКЛА)" : "";

        System.out.println("  Короткий код: " + link.getShortCode());
        System.out.println("  Исходный URL: " + link.getOriginalUrl());
        System.out.println("  Статус: " + status + expired);
        System.out.println("  Переходов: " + link.getClickCount() + " / " + link.getClickLimit() +
                " (осталось: " + link.getRemainingClicks() + ")");
        System.out.println("  Создана: " + DATE_FORMATTER.format(link.getCreatedAt()));
        System.out.println("  Истекает: " + DATE_FORMATTER.format(link.getExpiresAt()));

        if (link.isOwnedBy(currentUser.getId())) {
            System.out.println("  Владелец: Вы");
        }
    }
}

