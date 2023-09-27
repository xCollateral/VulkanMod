package net.vulkanmod;

import net.vulkanmod.config.Config;
import net.vulkanmod.config.VideoResolution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");
	private static String VERSION;
	public static Config CONFIG = loadConfig(VulkanModExpectPlatform.getConfigDirectory().resolve("vulkanmod_settings.json"));
	public static final String MODID = "vulkanmod";
	public static void onInitializeClient() {
		VERSION = VulkanModExpectPlatform.getVersion();
		LOGGER.info("== VulkanMod ==");
		VideoResolution.init();
	}

	private static Config loadConfig(Path path) {
		Config config = Config.load(path);
		if(config == null) {
			config = new Config();
			config.write();
		}
		return config;
	}

	public static String getVersion() {
		return VERSION;
	}
}