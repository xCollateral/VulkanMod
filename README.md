[![Download](https://img.shields.io/github/downloads/xCollateral/VulkanMod/total.svg)](https://github.com/xCollateral/VulkanMod/releases/)
# VulkanMod

This is a Fabric mod that re-writes the rendering engine in Vulkan.

Demonstration video: https://youtu.be/sbr7UxcAmOE

## Disclaimer

This software is in pre-alpha testing, I am not responsible for any damage that could occur to your hardware and software.
It is possible that this mod could cause damage to world saves, please be careful.

## Installation (Linux and Windows)

1) Move VulkanMod.jar inside mods folder of your minecraft instance.

## Installation for macOS

If the game crash, then follow these steps:  
1) Download `liblwjgl.dylib` and `liblwjgl_vma.dylib` from [here](https://www.lwjgl.org/browse/release/3.2.2/macosx/x64) and place them under `/Applications/MultiMC.app/Data/instances/[instance name]/natives `
2) Download the latest version of MoltenVK from the VulkanSDK or from [here](https://community.pcgamingwiki.com/files/file/2417-moltenvk-modified-with-dxvk-patches-for-macos-libmoltenvkdylib/) and place `libMoltenVK.dylib` on the natives folder
3) Download [shaderc](https://storage.googleapis.com/shaderc/badges/build_link_macos_clang_release.html)  and in the install/lib folder that was downloaded copy `libshaderc_shared.1.dylib` and rename it to `shaderc.dylib` and place it under your `natives/libwindows/x64/org/lwjgl/shaderc/` (create the needed subdirectories)
4) Place a second copy of `liblwjgl.dylib` under your shaderc folder
5) Your game should run now, side note, MultiMC deletes the natives folder after the game is done running so I'd reccomend you keep a copy that's setup with the stuff from above  
Credit: UnlikePaladin#5813
