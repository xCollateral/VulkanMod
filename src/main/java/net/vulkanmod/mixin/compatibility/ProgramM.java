package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import net.vulkanmod.gl.GlUtil;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Mixin(Program.class)
public class ProgramM {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static int compileShaderInternal(Program.Type type, String string, InputStream inputStream, String string2, GlslPreprocessor glslPreprocessor) throws IOException {
        String string3 = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        if (string3 == null) {
            throw new IOException("Could not load program " + type.getName());
        } else {
//            int i = GlStateManager.glCreateShader(type.getGlType());
//            GlStateManager.glShaderSource(i, glslPreprocessor.process(string3));
//            GlStateManager.glCompileShader(i);
//            if (GlStateManager.glGetShaderi(i, 35713) == 0) {
//                String string4 = StringUtils.trim(GlStateManager.glGetShaderInfoLog(i, 32768));
//                throw new IOException("Couldn't compile " + type.getName() + " program (" + string2 + ", " + string + ") : " + string4);
//            } else {
//                return i;
//            }

            //TODO maybe not needed?
            glslPreprocessor.process(string3);
            SPIRVUtils.compileShader(string2 + ":" + string, string3, GlUtil.extToShaderKind(type.getExtension()));
        }
        return 0;
    }
}
