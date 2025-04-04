package ru.l1ratch.whitelistbynicknames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class WhitelistByNicknames extends JavaPlugin implements Listener, CommandExecutor {
    private List<String> whitelistedPlayers;
    private Map<String, String> whitelistedUUIDs; // ник -> uuid
    private boolean whitelistEnabled;
    private boolean loggingEnabled;
    private boolean uuidCheckEnabled;
    private boolean botEnabled;
    private TelegramBotHandler telegramBot;
    private File configFile;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("wlbn").setExecutor(this);
        this.getCommand("wlbn").setTabCompleter(this); // Регистрация TabCompleter
        this.whitelistedPlayers = new ArrayList<>(getConfig().getStringList("whitelist"));
        this.whitelistedUUIDs = new HashMap<>();
        Map<String, Object> uuidMap = getConfig().getConfigurationSection("whitelistedUUIDs").getValues(false);
        for (Map.Entry<String, Object> entry : uuidMap.entrySet()) {
            whitelistedUUIDs.put(entry.getKey(), (String) entry.getValue());
        }
        this.whitelistEnabled = getConfig().getBoolean("whitelistEnabled", true);
        this.loggingEnabled = getConfig().getBoolean("loggingEnabled", true);
        this.uuidCheckEnabled = getConfig().getBoolean("uuidCheckEnabled", false);
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

        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();
        String playerIP = event.getAddress().getHostAddress();

        if (!whitelistedPlayers.contains(playerName)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.notWhitelisted", "&cВы не в белом списке.")));
            return;
        }

        // Сохранение UUID при первом входе
        if (!whitelistedUUIDs.containsKey(playerName)) {
            whitelistedUUIDs.put(playerName, playerUUID.toString());
            getConfig().set("whitelistedUUIDs." + playerName, playerUUID.toString());
            saveConfig();
            getLogger().info("Сохранен UUID " + playerUUID + " для игрока " + playerName + ".");
        } else if (uuidCheckEnabled) {
            // Проверка UUID при последующих входах
            String storedUUID = whitelistedUUIDs.get(playerName);
            if (!playerUUID.toString().equals(storedUUID)) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.uuidMismatch", "&cВаш UUID изменился. Обратитесь к администратору.")));
                getLogger().warning("UUID не совпадает для игрока " + playerName + ". Текущий: " + playerUUID + ", сохраненный: " + storedUUID + ".");
                return;
            }
        }

        if (loggingEnabled) {
            logPlayerLogin(playerName, playerIP, playerUUID.toString());
        }
    }

    public void logPlayerLogin(String playerName, String playerIP, String playerUUID) {
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
            String logEntry = String.format("%s | %s | %s | UUID: %s", playerName, new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()), playerIP, playerUUID);
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
            // При добавлении в белый список, сразу сохраняем UUID, если игрок онлайн
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && !whitelistedUUIDs.containsKey(playerName)) {
                whitelistedUUIDs.put(playerName, player.getUniqueId().toString());
                getConfig().set("whitelistedUUIDs." + playerName, player.getUniqueId().toString());
                saveConfig();
                getLogger().info("Сохранен UUID " + player.getUniqueId() + " для добавленного игрока " + playerName + ".");
            }
        }
    }

    public void removePlayerFromWhitelist(String playerName) {
        whitelistedPlayers.remove(playerName);
        getConfig().set("whitelist", whitelistedPlayers);
        getConfig().set("whitelistedUUIDs." + playerName, null); // Удаляем UUID при удалении из белого списка
        whitelistedUUIDs.remove(playerName);
        saveConfig();
    }

    public void setWhitelistEnabled(boolean enabled) {
        this.whitelistEnabled = enabled;
        getConfig().set("whitelistEnabled", enabled);
        saveConfig();
    }

    public void setUuidCheckEnabled(boolean enabled) {
        this.uuidCheckEnabled = enabled;
        getConfig().set("uuidCheckEnabled", enabled);
        saveConfig();
    }

    public void resetPlayerUUID(String playerName) {
        if (whitelistedUUIDs.containsKey(playerName)) {
            whitelistedUUIDs.remove(playerName);
            getConfig().set("whitelistedUUIDs." + playerName, null);
            saveConfig();
            getLogger().info("UUID для игрока " + playerName + " был сброшен.");
        } else {
            getLogger().warning("UUID для игрока " + playerName + " не найден.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wlbn.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Использование: /wlbn add <nick> | del <nick> | list | on | off | log | uuidcheck <on|off> | resetuuid <nick>");
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
            case "uuidcheck":
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /wlbn uuidcheck <on|off>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("on")) {
                    setUuidCheckEnabled(true);
                    sender.sendMessage(ChatColor.GREEN + "Проверка UUID включена.");
                } else if (args[1].equalsIgnoreCase("off")) {
                    setUuidCheckEnabled(false);
                    sender.sendMessage(ChatColor.GREEN + "Проверка UUID выключена.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Используйте: /wlbn uuidcheck <on|off>");
                }
                break;
            case "resetuuid":
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /wlbn resetuuid <ник>");
                    return true;
                }
                resetPlayerUUID(args[1]);
                sender.sendMessage(ChatColor.GREEN + "UUID для игрока " + args[1] + " был сброшен.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Неизвестная команда.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("wlbn.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("add", "del", "list", "on", "off", "log", "uuidcheck", "resetuuid").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "add":
                case "del":
                case "resetuuid":
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "uuidcheck":
                    return Arrays.asList("on", "off").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                default:
                    return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }
}