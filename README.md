# ♻ RecyclePlugin

Advanced and fully configurable recycling system for Paper servers.

RecyclePlugin allows players to recycle tools, armor, and weapons through a clean GUI system and receive configurable material rewards and XP.
Feel free to add me on discord (svncho_) for any problem or open an issue on [github](https://github.com/9ne7ven/RecyclePlugin)

---

## ✨ Features

- GUI-based recycling interface
- Fully configurable reward materials
- XP reward system
- Multi-language support (EN, FR, DE, ES, ZH)
- Automatic language fallback system
- Reload command
- Modern Adventure API text support
- Safe recycling logic (unregistered items protected)

---

## 🛠 Commands & Permissions


`/recycle`  
Open the recycling GUI.

Permission: `recycleplugin.use`  
Default: true


`/rpadmin reload`  
Reload the plugin configuration.

Permission: `recycleplugin.reload`  
Default: op

---

## 🔄 Update Checker

```yml
update-checker:
  enabled: true
  notify-console: true
  notify-ops-on-join: true

---

## ⚙ Configuration Example

```yaml
iron_sword:
  material: IRON_INGOT
  min: 0
  max: 2
```

Server owners can freely add any valid Minecraft material (based on Minecraft vanilla ID Items).

---

## 📦 Requirements


Paper 1.21+

Java 21


---

## 🚀 Roadmap

Economy (Vault) support

Durability-based scaling rewards

Custom model data support

PlaceholderAPI integration


---

## 📄 License

All Rights Reserved © svncho_


---
