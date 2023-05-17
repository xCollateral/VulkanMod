<div align='center'>

[![Logo](https://cdn.discordapp.com/attachments/851205250395930655/1087443536103882782/VKModBanner.png)](#)

![Download](https://img.shields.io/github/downloads/xCollateral/VulkanMod/total?color=red&logo=github&style=for-the-badge)
[![Discord](https://img.shields.io/badge/Discord-7289DA?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/FVXg7AYR2Q)
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/V7V7CHHJV)

A **pre-alpha** fabric mod that rewrites Minecraft's OpenGL renderer to use the Vulkan API.

</div>

### Downloads:

[![Modrinth](https://modrinth-utils.vercel.app/api/badge/downloads?id=vulkanmod&logo=true&style=for-the-badge)](https://modrinth.com/mod/vulkanmod)
[![CurseForge](https://cf.way2muchnoise.eu/title/635429_Get_Mod.svg?badge_style=flat)](https://www.curseforge.com/minecraft/mc-mods/vulkanmod)

### Requirements

- A Graphics Card that supports Vulkan 1.3***** or later.
- [Fabric loader](https://fabricmc.net/use/installer/)

## Steps:

There are none! Simply download the mod from one of the download buttons above and then move the JAR file into your **mods** folder like any other mod.

## Compatibility

Mod compatibility is not guaranteed. When VulkanMod is loaded, mods that make explicit calls to OpenGL will crash the game.
Check [Incompatible mods](https://github.com/xCollateral/VulkanMod/discussions/226) list to make sure you are not using one of them.

## Known Issues

- Linux and macOS may not work. We are working on ways to improve those platforms.

- Most mods that affect rendering in any way are likely to not work with this mod. Read note one for more info.

- Custom launchers are not recommended as they can cause issues with this mod. We are working on better compat for this but support is not guaranteed. 

## Notes
- If you come across any mod that do not work with this mod, please create an issue inside **this** repo and **do not** report it to the mod developer.

- Sodium will **NEVER** be supported! This is because it directly changes elements of Minecraft that we do as well and in turn would be a waste of time to use.

- If you need support/help with VulkanMod, or simply want to know when updates are released, you can join our Discord server [here](https://discord.gg/EDgQ88tJAk).
</div>
