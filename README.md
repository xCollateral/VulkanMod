# VulkanMod

This is a Fabric mod that re-writes the rendering engine in Vulkan. 

## Disclaimer

This software is in pre-alpha testing, I am not responsible for any damage that could occur to your hardware and software.
It is possible that this mod could cause damage to world saves, please be careful.

## Installation (Linux and Windows)

1) Move VulkanMod.jar inside mods folder of your minecraft instance.
2) Start the game, if it doesn't crash you're done.  
3) If it does, get your os's `lwjgl` binary.
  - For Linux get `liblwjgl.so` from [here](https://www.lwjgl.org/browse/release/3.2.2/linux/x64).
  - For Windows get `lwjgl.dll` from [here](https://www.lwjgl.org/browse/release/3.2.2/windows/x64)
4) Go to temp folder (Windows Win+R and type `%temp%`, `/tmp/` on Linux).
5) Find the `lwjgl[username]/3.2.2-SNAPSHOT/` folder.
6) Place the `lwjgl` binary there.
7) If you are on linux, open the jar and rename the `windows` folder to `libwindows`.
8) Start the game again.

## Installation for macOS

If the game crash, then follow these steps:  
1) Download `liblwjgl.dylib` and `liblwjgl_vma.dylib` from https://www.lwjgl.org/browse/release/3.2.2/macosx/x64and place them under `/Applications/MultiMC.app/Data/instances/[instance name]/natives `
2) Download the latest version of MoltenVK from the VulkanSDK or from here https://community.pcgamingwiki.com/files/file/2417-moltenvk-modified-with-dxvk-patches-for-macos-libmoltenvkdylib/ and place `libMoltenVK.dylib` on the natives folder
3) Download https://storage.googleapis.com/shaderc/badges/build_link_macos_clang_release.html  and in the install/lib folder that was downloaded copy `libshaderc_shared.1.dylib` and rename it to `shaderc.dylib` and place it under your `natives/libwindows/x64/org/lwjgl/shaderc/` (create the needed subdirectories)
4) Place a second copy of `liblwjgl.dylib` under your shaderc folder
5) Your game should run now, side note, MultiMC deletes the natives folder after the game is done running so I'd reccomend you keep a copy that's setup with the stuff from above
Credit: UnlikePaladin#5813
