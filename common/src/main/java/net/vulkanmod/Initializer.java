package net.vulkanmod;

import net.vulkanmod.config.Config;
import net.vulkanmod.config.VideoResolution;
import org.apache.commons.logging.impl.Jdk14Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dev.architectury.platform.Platform;

import java.nio.file.Path;

public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");

	private static String VERSION;
	public static Config CONFIG;

	public static final String MODID = "vulkanmod";

	public static void onInitializeClient() {

		VERSION = Platform.getMod("vulkanmod").getVersion();

		LOGGER.info("== VulkanMod ==");

		VideoResolution.init();

		var configPath = Platform.getConfigFolder().resolve("vulkanmod_settings.json");

		CONFIG = loadConfig(configPath);

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
