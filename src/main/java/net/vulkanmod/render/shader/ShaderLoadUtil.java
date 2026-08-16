package net.vulkanmod.render.shader;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.shaders.ShaderType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import net.vulkanmod.vulkan.shader.SpirvCompiler;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class ShaderLoadUtil {

    public static final String RESOURCES_PATH = SpirvCompiler.class.getResource("/assets/vulkanmod").toExternalForm();
    public static final String SHADERS_PATH = "%s/shaders/".formatted(RESOURCES_PATH);

    public static final Set<String> REMAPPED_SHADERS = Sets.newHashSet("core/screenquad.vsh",
                                                                       "core/rendertype_item_entity_translucent_cull.vsh",
                                                                       "core/animate_sprite.vsh",
                                                                       "core/animate_sprite_blit.fsh");

    public static String resolveShaderPath(String path) {
        return resolveShaderPath(SHADERS_PATH, path);
    }

    public static String resolveShaderPath(String shaderPath, String path) {
        return "%s%s".formatted(shaderPath, path);
    }

    public static String loadShader(String path, String shaderName) {
        return loadShader(path, shaderName, null);
    }

    public static String loadShader(String path, String shaderName, List<String> defines) {
        String source = getShaderSource(path, shaderName);

        if (defines != null && !defines.isEmpty()) {
            source = injectDefines(source, defines);
        }

        return source;
    }

    public static String getConfigFilePath(String path, String rendertype) {
        String basePath = "%s/shaders/%s".formatted(RESOURCES_PATH, path);
        String configPath = "%s/%s/%s.json".formatted(basePath, rendertype, rendertype);

        Path filePath;
        try {
            filePath = FileSystems.getDefault().getPath(configPath);

            if (!Files.exists(filePath)) {
                configPath = "%s/%s.json".formatted(basePath, rendertype);
                filePath = FileSystems.getDefault().getPath(configPath);
            }

            if (!Files.exists(filePath)) {
                return null;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return filePath.toString();
    }

    public static JsonObject getJsonConfig(String path, String rendertype) {
        // Check for external shader
        if (rendertype.contains(String.valueOf(Identifier.NAMESPACE_SEPARATOR))) {
            return null;
        }

        String basePath = path;
        String configPath = "%s/%s/%s.json".formatted(basePath, rendertype, rendertype);

        InputStream stream;
        try {
            stream = getInputStream(configPath);

            if (stream == null) {
                configPath = "%s/%s.json".formatted(basePath, rendertype);
                stream = getInputStream(configPath);
            }

            if (stream == null) {
                return null;
            }

            JsonElement jsonElement = JsonParser.parseReader(new BufferedReader(new InputStreamReader(stream)));
            stream.close();

            return (JsonObject) jsonElement;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public static String getShaderSource(Identifier resourceLocation, ShaderType type) {
        String shaderExtension = switch (type) {
            case VERTEX -> ".vsh";
            case FRAGMENT -> ".fsh";
        };

        String path = resourceLocation.getPath();
        String[] splitPath = splitPath(path);
        String shaderName = "%s%s".formatted(splitPath[1], shaderExtension);
        String shaderFile = "%s/shaders/%s/%s".formatted(RESOURCES_PATH, path, shaderName);

        InputStream stream;
        try {
            stream = getInputStream(shaderFile);

            if (stream == null) {
                shaderFile = "%s/shaders/%s%s".formatted(RESOURCES_PATH, path, shaderExtension);
                stream = getInputStream(shaderFile);
            }

            if (stream == null) {
                // Not found in VulkanMod's own jar. This RenderPipeline may belong to
                // another mod (e.g. malilib's custom terrain pipelines used by Litematica),
                // with its shader bundled in that mod's own jar. Resolve the Identifier's
                // namespace to its actual owning mod and look there instead of giving up.
                return getShaderSourceFromOwningMod(resourceLocation, shaderName, shaderExtension);
            }

            String source = IOUtils.toString(new BufferedReader(new InputStreamReader(stream)));
            stream.close();

            return source;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fallback for {@link #getShaderSource(Identifier, ShaderType)} when a shader isn't
     * found inside VulkanMod's own jar. Resolves {@code resourceLocation}'s namespace to its
     * owning mod via Fabric Loader and reads the shader straight from that mod's real
     * resource root (works the same in a dev environment or a packaged jar), mirroring the
     * same two-tier {@code path/name.ext} -> {@code path.ext} lookup order used above.
     */
    private static String getShaderSourceFromOwningMod(Identifier resourceLocation, String shaderName, String shaderExtension) {
        String namespace = resourceLocation.getNamespace();
        String path = resourceLocation.getPath();

        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(namespace);

        if (container.isEmpty()) {
            return null;
        }

        String[] candidates = {
            "assets/%s/shaders/%s/%s".formatted(namespace, path, shaderName),
            "assets/%s/shaders/%s%s".formatted(namespace, path, shaderExtension)
        };

        for (String candidate : candidates) {
            Optional<Path> found = container.get().findPath(candidate);

            if (found.isPresent() && Files.isRegularFile(found.get())) {
                try {
                    return Files.readString(found.get(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return null;
    }

    public static String getShaderSource(String path, ShaderType type) {
        String shaderExtension = switch (type) {
            case VERTEX -> ".vsh";
            case FRAGMENT -> ".fsh";
        };

        String[] splitPath = splitPath(path);
        String shaderName = "%s%s".formatted(splitPath[1], shaderExtension);

        String shaderFile = "%s/shaders/%s/%s".formatted(RESOURCES_PATH, path, shaderName);

        InputStream stream;
        try {
            stream = getInputStream(shaderFile);
            String source = IOUtils.toString(new BufferedReader(new InputStreamReader(stream)));
            stream.close();

            return source;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String getShaderSource(String path, String shaderName) {
        String shaderFile = "%s/%s".formatted(path, shaderName);

        InputStream stream;
        try {
            stream = getInputStream(shaderFile);

            if (stream == null) {
                return null;
            }

            String source = IOUtils.toString(new BufferedReader(new InputStreamReader(stream)));
            stream.close();

            return source;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String getShaderSource(String path, String configName, String shaderName, SpirvCompiler.ShaderKind type) {
        String shaderExtension = switch (type) {
            case VERTEX_SHADER -> ".vsh";
            case FRAGMENT_SHADER -> ".fsh";
            case COMPUTE_SHADER -> ".comp";
            default -> throw new UnsupportedOperationException("shader type %s unsupported");
        };

        String basePath = path;

        String shaderPath = "/%s/%s".formatted(configName, configName);
        String shaderFile = "%s%s%s".formatted(basePath, shaderPath, shaderExtension);

        InputStream stream;
        try {
            stream = getInputStream(shaderFile);

            if (stream == null) {
                shaderPath = "/%s".formatted(shaderName);
                shaderFile = "%s%s%s".formatted(basePath, shaderPath, shaderExtension);
                stream = getInputStream(shaderFile);
            }

            if (stream == null) {
                shaderPath = "/%s/%s".formatted(configName, shaderName);
                shaderFile = "%s%s%s".formatted(basePath, shaderPath, shaderExtension);
                stream = getInputStream(shaderFile);
            }

            if (stream == null) {
                shaderPath = "/%s/%s".formatted(shaderName, shaderName);
                shaderFile = "%s%s%s".formatted(basePath, shaderPath, shaderExtension);
                stream = getInputStream(shaderFile);
            }

            if (stream == null) {
                return null;
            }

            String source = IOUtils.toString(new BufferedReader(new InputStreamReader(stream)));
            stream.close();

            return source;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFileName(String path) {
        int idx = path.lastIndexOf('/');
        return idx > -1 ? path.substring(idx + 1) : path;
    }

    public static String removeNameSpace(String path) {
        int idx = path.indexOf(':');
        return idx > -1 ? path.substring(idx + 1) : path;
    }

    public static String[] splitPath(String path) {
        int idx = path.lastIndexOf('/');

        return new String[] {path.substring(0, idx), path.substring(idx + 1)};
    }

    public static InputStream getInputStream(String path) {
        try {
            var path1 = Paths.get(new URI(path));

            if (!Files.exists(path1))
                return null;

            return Files.newInputStream(path1);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String injectDefines(String shaderSrc, List<String> defines) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String define : defines) {
            stringBuilder.append("#define ").append(define).append('\n');
        }

        int i = shaderSrc.indexOf('\n');

        String out = shaderSrc.substring(0, i + 1) + "\n" + stringBuilder + "\n" + shaderSrc.substring(i + 1);
        return out;
    }
}
