# VulkanMod

This is a Fabric mod that re-writes the rendering engine in Vulkan. 

## Disclaimer

This software is in pre-alpha testing, I am not responsible for any damage that could occur to your hardware and software.
It is possible that this mod could cause damage to world saves, please be careful.

## Installation

1) Move VulkanMod.jar inside mods folder of your minecraft instance.
2) Start the game, it may crash. 
 - If it does, get your os's `lwjgl` binary.
   - For Linux get `liblwjgl.so` from [here](https://www.lwjgl.org/browse/release/3.2.2/linux/x64).
   - For Windows get `lwjgl.dll` from [here](https://www.lwjgl.org/browse/release/3.2.2/windows/x64).
 - Go to temp folder (Windows Win+R and type `%temp%`, `/tmp/` on Linux).
 - Find the `lwjgl[username]/3.2.2-SNAPSHOT/` folder.
 - Place the `lwjgl` binary there.
 - If you are on linux, open the jar and rename the `windows` folder to `libwindows`.