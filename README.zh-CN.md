<div align="center">
  
# 🚓 ServerKara

**适用于 Leaf/Paper 1.21.11 的高级罚款与守卫管理系统**

[🇺🇸 English](README.md) | [🇷🇺 Русский](README.ru.md) | [🇨🇳 中文](README.zh-CN.md)

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=java)](https://adoptium.net/)
[![Paper API](https://img.shields.io/badge/Paper-1.21.11-blue.svg?style=for-the-badge)](https://papermc.io/)

</div>

---

ServerKara 是一款专为运行 **Leaf/Paper 1.21.11** 的 Minecraft 服务器设计的全面角色扮演 (Role-Play) 管理插件。它引入了完整的罚款系统、紧急守卫呼叫系统 (`/911`)，以及为守卫和管理员提供的高度直观的交互式 GUI 菜单。

## ✨ 核心特性

- 💸 **罚款系统：** 可向玩家开出罚单，并自定义金额、截止期限和原因（支持逾期通知）。
- 🚨 **紧急呼叫：** 玩家可以通过 `/911 <原因>` 呼叫守卫。守卫会立即收到求救信息及精确的世界坐标。
- 📱 **交互式 GUI：** 使用 `/karamenu` 指令打开统一管理面板，可处理活跃呼叫、追踪欠款人、查看守卫名单以及审批入职申请。
- ⚡ **快捷操作：** 在菜单中 `Shift+右键` 将罚单标记为已支付，`左键` 呼叫记录可直接传送到案发现场。
- 👮 **守卫招募：** 玩家可以通过 `/guard apply` 提交申请，护卫长可直接在 GUI 中通过/拒绝申请，完全不需要依赖 LuckPerms 等外部权限插件！
- 📂 **无需数据库：** 所有数据（罚单、呼叫记录、申请表）均安全地保存在本地 YAML 文件中。无需配置 Vault 或外置数据库！

## 💻 插件指令

| 指令 | 描述 |
|---|---|
| `/kara <玩家> <金额> <时间> <原因>` | 向玩家开出罚单 |
| `/kara list [玩家]` | 查看自己或他人的罚单记录 |
| `/kara paid <ID>` | 将指定罚单标记为已支付 |
| `/911 <描述>` | 呼叫守卫 (紧急求助) |
| `/karamenu` | 打开交互式守卫管理 GUI |
| `/guard apply` | 提交加入守卫的申请 |
| `/guard add/remove <玩家>` | 批准/踢出守卫 |
| `/guard setchief/removechief <玩家>` | 任命/撤销护卫长职务 |

*时间格式示例：`30m` (分钟), `2h` (小时), `3d` (天), `1w` (周), `1d12h`。*

## 🚀 安装指南

1. 确保您的服务器正在运行 **Java 21** 和 **Paper/Leaf 1.21.11**。
2. 下载 `AurionKara.jar`（或从源码编译），并将其放入您的 `plugins` 文件夹中。
3. 重启服务器。
4. 打开 `plugins/AurionKara/config.yml` 配置文件，设置初始的护卫长（Chief Guards）。

## 🛠️ 源码编译
```bash
gradle build
```
编译完成的插件将生成在 `build/libs/` 目录中。
