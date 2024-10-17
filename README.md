# WhitelistByNicknames
WhitelistByNicknames - Плагин на удобный Whitelist, работающий по никнеймам, а не по uuid.<br>
У плагина так же есть функции:
- Логирование входа игроков в формате nikname | date | ip-address
- Телеграм бот для добавления, удаления и получения списка игроков без консоли и не входя в игру


## Конфигурация плагина
```
whitelistEnabled: true
loggingEnabled: true
botEnabled: false
botToken: "YOUR_BOT_TOKEN"
botUsername: "YOUR_BOT_USERNAME"
allowedUsers: [] # Укажите ID пользователей, которым разрешен доступ. Если пусто, доступ открыт всем.
whitelist: []
messages:
  notWhitelisted: "&cВы не в белом списке."
```

## Команды и права
### Команды:
- `/wlbn add 'nick'` - Добавить игрока в белый список
- `/wlbn del 'nick` - Удалить уигрока из белого списка
- `/wlbn on | off` - Включить/Выключить белый список
- `/wlbn list` - Вывести список игроков из белого списка
- `/wlbn log` - Вкл/Выкл логирование входа(toggle command)

### Права:
- `wlbn.admin` - Дает доступ к командам плагина whitelistbynicknames

### Команды телеграм бота:
- `/add 'nick'` - Добавить игрока в белый список
- `/del 'nick'` - Удалить уигрока из белого списка
- `/on` - Включить белый список
- `/off` - Выключить белый список
- `/list` - Вывести список игроков из белого списка
