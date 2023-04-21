package net.vulkanmod.vulkan.shader.parser;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.Sampler;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.UBO;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class UniformParser {

    private final GlslConverter converterInstance;
    private final StageUniforms[] stageUniforms = new StageUniforms[GlslConverter.ShaderStage.values().length];
    private StageUniforms currentUniforms;
    List<Uniform> globalUniforms = new ArrayList<>();

    private int currentLocation = 1;
    private String type;
    private String name;

    public UniformParser(GlslConverter converterInstance) {
        this.converterInstance = converterInstance;

        for(int i = 0; i < this.stageUniforms.length; ++i) {
            this.stageUniforms[i] = new StageUniforms();
        }
    }

    public boolean parseToken(String token, GlslConverter.ShaderStage vertex) {
        if(token.matches("uniform")) return false;

        if (this.type == null) {
            this.type = token;

        }
        else if (this.name == null) {
            token = removeSemicolon(token);

            this.name = token;

            //TODO check if already present
            Uniform uniform = new Uniform(this.type, this.name);
            if ("sampler2D".equals(this.type)) {
                final boolean b = vertex == GlslConverter.ShaderStage.Vertex;
                Sampler sampler = new Sampler(getBindingValueToken(token),
                        b ? VK_SHADER_STAGE_VERTEX_BIT:VK_SHADER_STAGE_FRAGMENT_BIT,
                        b ? VK_PIPELINE_STAGE_VERTEX_SHADER_BIT:VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                        this.type, this.name);
                if (!this.currentUniforms.samplers.contains(sampler))
                    this.currentUniforms.samplers.add(sampler);
            } else {
                if (!this.globalUniforms.contains(uniform))
                    this.globalUniforms.add(uniform);
            }

            this.resetSate();
            return true;
        }

        return false;
    }

    private int getBindingValueToken(String token) {
        final char c = token.charAt(token.indexOf("=") + 2);
        return switch(c)
                {
                    case '0' -> 0;
                    case '1' -> 1;
                    case '2' -> 2;
                    case '3' -> 3;
                    default -> throw new IllegalStateException("Unexpected value: " + c);
                };
    }

    public void setCurrentUniforms(GlslConverter.ShaderStage shaderStage) {
        this.currentUniforms = stageUniforms[shaderStage.ordinal()];
    }

    private void resetSate() {
        this.type = null;
        this.name = null;
//        this.state = State.None;
    }

    public String createUniformsCode() {
        StringBuilder builder = new StringBuilder();

        //hardcoded 0 binding as it should always be 0 in this case
        builder.append(String.format("layout(binding = %d) uniform UniformBufferObject {\n", 0));
        for(Uniform uniform : this.globalUniforms) {
            builder.append(String.format("%s %s;\n", uniform.type, uniform.name));
        }
        builder.append("};\n\n");

        return builder.toString();
    }

    public String createSamplersCode(GlslConverter.ShaderStage shaderStage) {
        StringBuilder builder = new StringBuilder();

        //TODO find a better way for bindings
        for(Sampler sampler : this.stageUniforms[shaderStage.ordinal()].samplers) {
            builder.append(String.format("layout(binding = %d) uniform %s %s;\n", sampler.binding(), sampler.type(), sampler.name()));
            this.currentLocation++;
        }
        builder.append("\n");

        return builder.toString();
    }

    public UBO createUBO() {
        AlignedStruct.Builder builder = new AlignedStruct.Builder();

        for(Uniform uniform : this.globalUniforms) {
            builder.addFieldInfo(uniform.type, uniform.name);
        }

        //hardcoded 0 binding as it should always be 0 in this case
        return builder.buildUBO(0, Pipeline.Builder.getTypeFromString("all"));
    }

    public List<Sampler> createSamplerList() {
        List<Sampler> samplers = new ObjectArrayList<>();

        for(StageUniforms stageUniforms : this.stageUniforms) {
            samplers.addAll(stageUniforms.samplers);
        }

        return samplers;
    }

    public static String removeSemicolon(String s) {
        int last = s.length() - 1;
        if((s.charAt(last)) != ';' )
            throw new IllegalArgumentException("last char is not ;");
        return s.substring(0, last);
    }

    public record Uniform(String type, String name) {}

    private static class StageUniforms {
        List<Sampler> samplers = new ArrayList<>();
    }

    enum State {
        Uniform,
        Sampler,
        None
    }
}
