package com.linkshorter;

import com.linkshorter.cli.CommandProcessor;
import com.linkshorter.config.AppConfiguration;
import com.linkshorter.model.User;
import com.linkshorter.repository.LinkRepository;
import com.linkshorter.service.*;

import java.util.Scanner;

/**
 * Main entry point for the Link Shortener application
 */
public class Main {
    private static final String BANNER = """
            ╔══════════════════════════════════════════════════════════════════════╗
            ║                                                                      ║
            ║              СЕРВИС СОКРАЩЕНИЯ ССЫЛОК / LINK SHORTENER               ║
            ║                                                                      ║
            ║              Создавайте короткие ссылки с лимитами                   ║
            ║              и автоматическим управлением временем жизни             ║
            ║                                                                      ║
            ╚══════════════════════════════════════════════════════════════════════╝
            """;

    public static void main(String[] args) {
        System.out.println(BANNER);

        // Initialize application
        AppConfiguration config = new AppConfiguration();
        LinkRepository repository = new LinkRepository();
        ShortCodeGenerator codeGenerator = new ShortCodeGenerator(config.getShortCodeLength());
        NotificationService notificationService = new NotificationService(config.isNotificationsEnabled());
        LinkService linkService = new LinkService(repository, codeGenerator, notificationService, config);

        // Start cleanup scheduler
        CleanupScheduler cleanupScheduler = new CleanupScheduler(linkService, config);
        cleanupScheduler.start();

        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nЗавершение работы...");
            cleanupScheduler.stop();
        }));

        // Initialize CLI
        CommandProcessor commandProcessor = new CommandProcessor(linkService);

        // Get or create user
        User user = getUserFromArgs(args);
        commandProcessor.setCurrentUser(user);

        System.out.println("\nДобро пожаловать!");
        System.out.println("Ваш UUID: " + user.getIdString());
        System.out.println("\nВведите 'help' для просмотра доступных команд.");
        System.out.println("Введите 'exit' для выхода.\n");

        // Start CLI loop
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if (input.trim().equalsIgnoreCase("exit")) {
                break;
            }

            commandProcessor.processCommand(input);
        }

        scanner.close();
        cleanupScheduler.stop();
        System.out.println("До свидания!");
    }

    private static User getUserFromArgs(String[] args) {
        if (args.length > 0) {
            try {
                return User.fromId(args[0]);
            } catch (IllegalArgumentException e) {
                System.out.println("✗ Неверный формат UUID. Создан новый пользователь.");
            }
        }
        return User.createNew();
    }
}

