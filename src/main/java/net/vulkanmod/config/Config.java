package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class Config {

    public int frameQueueSize = 2;
    public VideoResolution resolution = VideoResolution.getFirstAvailable();
    public boolean xWayland = false;
    public boolean windowedFullscreen = false;

    public boolean guiOptimizations = false;
    public int advCulling = 2;
    public boolean indirectDraw = false;

    public boolean perRenderTypeAreaBuffers = false;
    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public int device = -1;

    public int ambientOcclusion = 1;

    private static Path path;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();


    public static Config load(Path path) {
        Config config;
        Config.path = path;

        if (Files.exists(path)) {
            try (FileReader fileReader = new FileReader(path.toFile())) {
                config = GSON.fromJson(fileReader, Config.class);
            }
            catch (IOException exception) {
                throw new RuntimeException(exception.getMessage());
            }
        }
        else {
            config = null;
        }

        return config;
    }

    public void write() {

        if(!Files.exists(path.getParent())) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Files.write(path, Collections.singleton(GSON.toJson(this)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
