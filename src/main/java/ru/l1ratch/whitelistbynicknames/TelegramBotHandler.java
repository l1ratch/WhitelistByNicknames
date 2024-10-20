package ru.l1ratch.whitelistbynicknames;

import org.bukkit.Bukkit;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TelegramBotHandler extends TelegramLongPollingBot {
    private final WhitelistByNicknames plugin;
    private final String botToken;
    private final String botUsername;
    private final List<Long> allowedUsers;
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 5000; // 5 секунд

    public TelegramBotHandler(WhitelistByNicknames plugin, String botToken, String botUsername, List<Long> allowedUsers) {
        this.plugin = plugin;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.allowedUsers = allowedUsers;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            Long userId = message.getFrom().getId();

            // Проверка доступа: если список allowedUsers пуст, доступ разрешен всем
            if (allowedUsers != null && !allowedUsers.isEmpty() && !allowedUsers.contains(userId)) {
                sendMessageWithRetry(chatId, "У вас нет прав для управления белым списком.");
                return;
            }

            String[] args = message.getText().split(" ");
            if (args.length < 1) {
                sendMessageWithRetry(chatId, "Используйте команды: /add <nick>, /del <nick>, /on, /off, /list");
                return;
            }

            switch (args[0].toLowerCase()) {
                case "/add":
                    if (args.length != 2) {
                        sendMessageWithRetry(chatId, "Используйте: /add <nick>");
                    } else {
                        plugin.addPlayerToWhitelist(args[1]);
                        sendMessageWithRetry(chatId, "Игрок " + args[1] + " добавлен в белый список.");
                    }
                    break;
                case "/del":
                    if (args.length != 2) {
                        sendMessageWithRetry(chatId, "Используйте: /del <nick>");
                    } else {
                        plugin.removePlayerFromWhitelist(args[1]);
                        sendMessageWithRetry(chatId, "Игрок " + args[1] + " удален из белого списка.");
                    }
                    break;
                case "/on":
                    plugin.setWhitelistEnabled(true);
                    sendMessageWithRetry(chatId, "Белый список включен.");
                    break;
                case "/off":
                    plugin.setWhitelistEnabled(false);
                    sendMessageWithRetry(chatId, "Белый список выключен.");
                    break;
                case "/list":
                    String whitelist = String.join(", ", plugin.getWhitelistedPlayers());
                    sendMessageWithRetry(chatId, "Белый список: " + whitelist);
                    break;
                default:
                    sendMessageWithRetry(chatId, "Неизвестная команда. Используйте: /add <nick>, /del <nick>, /on, /off, /list");
            }
        }
    }

    private void sendMessageWithRetry(String chatId, String text) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(text);
                execute(message);
                return; // Успешно отправлено, выход из метода
            } catch (TelegramApiException e) {
                attempt++;
                plugin.getLogger().warning("Не удалось отправить сообщение в Telegram (попытка " + attempt + " из " + MAX_RETRIES + "): " + e.getMessage());
                if (attempt >= MAX_RETRIES) {
                    plugin.getLogger().severe("Превышено количество попыток отправки сообщения в Telegram. Сообщение не было отправлено.");
                    break;
                }
                try {
                    // Подождать перед следующей попыткой
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}