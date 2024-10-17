package ru.l1ratch.whitelistbynicknames;

import org.bukkit.Bukkit;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class TelegramBotHandler extends TelegramLongPollingBot {
    private final WhitelistByNicknames plugin;
    private final String botToken;
    private final String botUsername;
    private final List<Long> allowedUsers;

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
                sendMessage(chatId, "У вас нет прав для управления белым списком.");
                return;
            }

            String[] args = message.getText().split(" ");
            if (args.length < 1) {
                sendMessage(chatId, "Используйте команды: /add <nick>, /del <nick>, /on, /off, /list");
                return;
            }

            switch (args[0].toLowerCase()) {
                case "/add":
                    if (args.length != 2) {
                        sendMessage(chatId, "Используйте: /add <nick>");
                    } else {
                        plugin.addPlayerToWhitelist(args[1]);
                        sendMessage(chatId, "Игрок " + args[1] + " добавлен в белый список.");
                    }
                    break;
                case "/del":
                    if (args.length != 2) {
                        sendMessage(chatId, "Используйте: /del <nick>");
                    } else {
                        plugin.removePlayerFromWhitelist(args[1]);
                        sendMessage(chatId, "Игрок " + args[1] + " удален из белого списка.");
                    }
                    break;
                case "/on":
                    plugin.setWhitelistEnabled(true);
                    sendMessage(chatId, "Белый список включен.");
                    break;
                case "/off":
                    plugin.setWhitelistEnabled(false);
                    sendMessage(chatId, "Белый список выключен.");
                    break;
                case "/list":
                    String whitelist = String.join(", ", plugin.getWhitelistedPlayers());
                    sendMessage(chatId, "Белый список: " + whitelist);
                    break;
                default:
                    sendMessage(chatId, "Неизвестная команда. Используйте: /add <nick>, /del <nick>, /on, /off, /list");
            }
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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