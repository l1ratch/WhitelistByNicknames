# WhitelistByNicknames
WhitelistByNicknames - Плагин на удобный Whitelist, работающий по никнеймам, а не по uuid.<br>
У плагина так же есть функции:
- Логирование входа игроков в формате nikname | date | ip-address
- Телеграм бот для добавления, удаления и получения списка игроков без консоли и не входя в игру


## Конфигурация плагина
```
# ОБЩИЕ НАСТРОЙКИ
whitelistEnabled: true
loggingEnabled: true
uuidCheckEnabled: false # Добавлена настройка для включения/выключения проверки UUID

# НАСТРОЙКИ БОТА (telegram)
botEnabled: false
botToken: "YOUR_BOT_TOKEN"
botUsername: "YOUR_BOT_USERNAME"
allowedUsers: [] # Укажите ID пользователей, которым разрешен доступ. Если пусто, доступ открыт всем.

# СПИСКИ
whitelist: []
whitelistedUUIDs: {} # Здесь будут храниться UUID игроков из белого списка

# НАСТРОЙКА СООБЩЕНИЙ
messages:
  notWhitelisted: "&cВы не в белом списке."
  uuidMismatch: "&cВаш UUID изменился. Обратитесь к администратору." # Сообщение при несовпадении UUID
```

## Команды и права
### Команды:
- `/wlbn add 'nick'` - Добавить игрока в белый список
- `/wlbn del 'nick` - Удалить уигрока из белого списка
- `/wlbn on | off` - Включить/Выключить белый список
- `/wlbn list` - Вывести список игроков из белого списка
- `/wlbn log` - Вкл/Выкл логирование входа(toggle command)
- `/wlbn uuidcheck on | off` - Вкл/Выкл проверку по UUID
- `/wlbn resetuuid 'nick'` - Сбросить UUID игрока

### Права:
- `wlbn.admin` - Дает доступ к командам плагина whitelistbynicknames

### Команды телеграм бота:
- `/add 'nick'` - Добавить игрока в белый список
- `/del 'nick'` - Удалить уигрока из белого списка
- `/on` - Включить белый список
- `/off` - Выключить белый список
- `/list` - Вывести список игроков из белого списка
