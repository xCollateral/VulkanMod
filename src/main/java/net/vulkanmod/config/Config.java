package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class Config {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static Path CONFIG_PATH;
    public int frameQueueSize = 2;
    public boolean windowedFullscreen = false;
    public int advCulling = 2;
    public boolean indirectDraw = true;
    public boolean backFaceCulling = true;
    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public int device = -1;
    public int ambientOcclusion = 1;

    public static Config load(Path path) {
        CONFIG_PATH = path;
        try (FileReader fileReader = new FileReader(path.toFile())) {
            return GSON.fromJson(fileReader, Config.class);
        } catch (IOException exception) {
            return null;
        }
    }

    public void write() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.write(CONFIG_PATH, Collections.singleton(GSON.toJson(this)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
