<div align='center'>

[![Logo](https://media.discordapp.net/attachments/963349566839738369/969920960373334076/Vlogo.png?width=300&height=300)](#)

# VulkanMod

This is a fabric mod that rewrites Minecraft OpenGL renderer to use Vulkan.

[![Download](https://img.shields.io/github/downloads/xCollateral/VulkanMod/total?color=red&logo=github&style=for-the-badge)](https://github.com/xCollateral/VulkanMod/releases/)

[Demostration Video](https://youtu.be/sbr7UxcAmOE)

</div>

## Installation (Windows and Linux)

- Move VulkanMod.jar to the Minecraft `mods` folder of your Minecraft Instance.

## Installation for macOS

If the game crash, then follow these steps:  
1) Download `liblwjgl.dylib` and `liblwjgl_vma.dylib` from [here](https://www.lwjgl.org/browse/release/3.2.2/macosx/x64) and place them under `/Applications/MultiMC.app/Data/instances/[instance name]/natives `
2) Download the latest version of MoltenVK from the VulkanSDK or from [here](https://community.pcgamingwiki.com/files/file/2417-moltenvk-modified-with-dxvk-patches-for-macos-libmoltenvkdylib/) and place `libMoltenVK.dylib` on the natives folder
3) Download [shaderc](https://storage.googleapis.com/shaderc/badges/build_link_macos_clang_release.html)  and in the install/lib folder that was downloaded copy `libshaderc_shared.1.dylib` and rename it to `shaderc.dylib` and place it under your `natives/libwindows/x64/org/lwjgl/shaderc/` (create the needed subdirectories)
4) Place a second copy of `liblwjgl.dylib` under your shaderc folder
5) Your game should run now, side note, MultiMC deletes the natives folder after the game is done running so I'd reccomend you keep a copy that's setup with the stuff from above  
Credit: UnlikePaladin#5813
