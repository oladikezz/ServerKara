<div align="center">
  
# 🚓 ServerKara

**Advanced fines and guard management system for Leaf/Paper 1.21.11**

[🇺🇸 English](README.md) | [🇷🇺 Русский](README.ru.md) | [🇨🇳 中文](README.zh-CN.md)

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=java)](https://adoptium.net/)
[![Paper API](https://img.shields.io/badge/Paper-1.21.11-blue.svg?style=for-the-badge)](https://papermc.io/)

</div>

---

ServerKara is a comprehensive role-play moderation plugin for Minecraft servers running **Leaf/Paper 1.21.11**. It introduces an integrated fines system, an emergency guard calling system (`/911`), and a highly intuitive interactive GUI for guards and admins.

## ✨ Features

- 💸 **Fines System:** Issue fines to players with configurable amounts, deadlines, and reasons.
- 🚨 **Emergency Calls:** Players can type `/911 <reason>` to summon guards. Guards receive the message, world, and exact coordinates.
- 📱 **Interactive GUI:** The `/karamenu` command opens a unified interface to manage active calls, track debtors, view the guard roster, and handle recruitment applications.
- ⚡ **Quick Actions:** Shift+Right-Click to mark fines as paid, Left-Click to teleport directly to an emergency call.
- 👮 **Guard Recruitment:** Players can apply via `/guard apply`, and Chief Guards can accept/reject them through the GUI without needing external permission plugins like LuckPerms.
- 📂 **No Database Required:** All data (fines, calls, applications) is stored safely in a local YAML file. No Vault or database setup needed!

## 💻 Commands

| Command | Description |
|---|---|
| `/kara <player> <amount> <time> <reason>` | Issue a fine to a player |
| `/kara list [player]` | View your own fines, or another player's fines |
| `/kara paid <ID>` | Mark a specific fine as paid |
| `/911 <description>` | Call for the guards (Emergency) |
| `/karamenu` | Open the interactive Guard GUI |
| `/guard apply` | Submit an application to join the guards |
| `/guard add/remove <player>` | Accept/Remove a player from the guards |
| `/guard setchief/removechief <player>` | Promote/Demote a Chief Guard |

*Time format examples: `30m` (minutes), `2h` (hours), `3d` (days), `1w` (week), `1d12h`.*

## 🚀 Installation

1. Ensure your server runs **Java 21** and **Paper/Leaf 1.21.11**.
2. Download `AurionKara.jar` (or compile from source) and place it in your `plugins` folder.
3. Restart your server.
4. Open `plugins/AurionKara/config.yml` to define your starting Chief Guards.

## 🛠️ Building from source
```bash
gradle build
```
The compiled plugin will be available in the `build/libs` directory.
