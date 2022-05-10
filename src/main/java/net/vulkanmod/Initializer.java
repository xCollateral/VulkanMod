package net.vulkanmod;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static org.objectweb.asm.Opcodes.*;

public class Initializer implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");

	@Override
	public void onInitialize() {

		LOGGER.info("Hello Fabric world!");
	}
}
