package ru.l1ratch.whitelistbynicknames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WhitelistByNicknames extends JavaPlugin implements Listener, CommandExecutor {
    private List<String> whitelistedPlayers;
    private boolean whitelistEnabled;
    private boolean loggingEnabled;
    private boolean botEnabled;
    private TelegramBotHandler telegramBot;
    private File configFile;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("wlbn").setExecutor(this);
        this.whitelistedPlayers = new ArrayList<>(getConfig().getStringList("whitelist"));
        this.whitelistEnabled = getConfig().getBoolean("whitelistEnabled", true);
        this.loggingEnabled = getConfig().getBoolean("loggingEnabled", true);
        this.botEnabled = getConfig().getBoolean("botEnabled", false);
        this.configFile = new File(getDataFolder(), "config.yml");

        if (botEnabled) {
            initTelegramBot();
        }

        getLogger().info("WhitelistByNicknames плагин включен.");
    }

    @Override
    public void onDisable() {
        getLogger().info("WhitelistByNicknames плагин выключен.");
    }

    private void initTelegramBot() {
        String botToken = getConfig().getString("botToken");
        String botUsername = getConfig().getString("botUsername");
        List<Long> allowedUsers = getConfig().getLongList("allowedUsers");

        if (botToken == null || botUsername == null) {
            getLogger().warning("Telegram бот не настроен. Проверьте конфигурацию.");
            return;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBot = new TelegramBotHandler(this, botToken, botUsername, allowedUsers);
            botsApi.registerBot(telegramBot);
            getLogger().info("Telegram бот успешно инициализирован.");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!whitelistEnabled) return;

        String playerName = event.getPlayer().getName();
        String playerIP = event.getAddress().getHostAddress();
        if (!whitelistedPlayers.contains(playerName)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.notWhitelisted", "&cВы не в белом списке.")));
            return;
        }

        if (loggingEnabled) {
            logPlayerLogin(playerName, playerIP);
        }
    }

    public void logPlayerLogin(String playerName, String playerIP) {
        if (!loggingEnabled) {
            return; // Логирование отключено, пропускаем запись лога
        }

        String logDir = getDataFolder() + "/logs/";
        File dir = new File(logDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String dateStr = new SimpleDateFormat("dd-MM-yy").format(new Date());
        String logFilePath = logDir + "logs_" + dateStr + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            String logEntry = String.format("%s | %s | %s", playerName, new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()), playerIP);
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            getLogger().warning("Не удалось записать лог: " + e.getMessage());
        }
    }

    public List<String> getWhitelistedPlayers() {
        return whitelistedPlayers;
    }

    public void addPlayerToWhitelist(String playerName) {
        if (!whitelistedPlayers.contains(playerName)) {
            whitelistedPlayers.add(playerName);
            getConfig().set("whitelist", whitelistedPlayers);
            saveConfig();
        }
    }

    public void removePlayerFromWhitelist(String playerName) {
        whitelistedPlayers.remove(playerName);
        getConfig().set("whitelist", whitelistedPlayers);
        saveConfig();
    }

    public void setWhitelistEnabled(boolean enabled) {
        this.whitelistEnabled = enabled;
        getConfig().set("whitelistEnabled", enabled);
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wlbn.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Использование: /wlbn add <nick> | del <nick> | list | on | off | log");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /wlbn add <nick>");
                    return true;
                }
                addPlayerToWhitelist(args[1]);
                sender.sendMessage(ChatColor.GREEN + "Игрок " + args[1] + " добавлен в белый список.");
                break;
            case "del":
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /wlbn del <nick>");
                    return true;
                }
                removePlayerFromWhitelist(args[1]);
                sender.sendMessage(ChatColor.GREEN + "Игрок " + args[1] + " удален из белого списка.");
                break;
            case "list":
                sender.sendMessage(ChatColor.GREEN + "Белый список: " + String.join(", ", whitelistedPlayers));
                break;
            case "on":
                setWhitelistEnabled(true);
                sender.sendMessage(ChatColor.GREEN + "Белый список включен.");
                break;
            case "off":
                setWhitelistEnabled(false);
                sender.sendMessage(ChatColor.GREEN + "Белый список выключен.");
                break;
            case "log":
                loggingEnabled = !loggingEnabled;
                getConfig().set("loggingEnabled", loggingEnabled);
                saveConfig();
                String status = loggingEnabled ? "включено" : "выключено";
                sender.sendMessage(ChatColor.GREEN + "Логирование " + status + ".");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Неизвестная команда.");
        }
        return true;
    }
}