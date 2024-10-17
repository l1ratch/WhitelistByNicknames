# WhitelistByNicknames - 

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
- /wlbn add 'nick' - 
- /wlbn del 'nick - 
- /wlbn on | off -
- /wlbn list -
- /wlbn log - 
### Права:
wlbn.admin - Дает доступ к командам плагина whitelistbynicknames
