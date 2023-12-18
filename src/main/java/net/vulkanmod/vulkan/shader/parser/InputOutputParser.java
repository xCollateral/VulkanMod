package net.vulkanmod.vulkan.shader.parser;

import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Objects;

import static net.vulkanmod.vulkan.shader.parser.UniformParser.removeSemicolon;

public class InputOutputParser {
    private final GlslConverter converterInstance;
    private VertexFormat vertexFormat;

    private final AttributeSet vertInAttributes = new AttributeSet();
    private final AttributeSet vertOutAttributes = new AttributeSet();

    private GlslConverter.ShaderStage shaderStage;

    private int currentLocation = 0;
    private String ioType;
    private String type;
    private String name;

    public InputOutputParser(GlslConverter converterInstance) {
        this.converterInstance = converterInstance;
    }

    public boolean parseToken(String token) {

        if (this.ioType == null)
            this.ioType = token;
        else if (this.type == null)
            this.type = token;
        else if (this.name == null) {
            token = removeSemicolon(token);

            this.name = token;

            if(this.shaderStage == GlslConverter.ShaderStage.Vertex) {
                switch (this.ioType) {
                    case "in" -> this.vertInAttributes.add(this.type, this.name);
                    case "out" -> this.vertOutAttributes.add(this.type, this.name);
                }
            }
            else {
                switch (this.ioType) {
                    case "in" -> {
                        if(!this.vertOutAttributes.contains(this.type, this.name))
                            throw new RuntimeException("fragment in attribute does not match vertex output");
                    }
                    case "out" -> {
                        //TODO check output
                    }
                }
            }
            this.resetState();
            return true;
        }

        return false;
    }

    private void resetState() {
        this.ioType = null;
        this.type = null;
        this.name = null;
    }

    public String createInOutCode() {
        //TODO
        StringBuilder builder = new StringBuilder();

        if(this.shaderStage == GlslConverter.ShaderStage.Vertex) {
            //In
            for(Attribute attribute : this.vertInAttributes.attributes) {
                builder.append(String.format("layout(location = %d) in %s %s;\n", attribute.location, attribute.type, attribute.name));
            }
            builder.append("\n");

            //Out
            for(Attribute attribute : this.vertOutAttributes.attributes) {
                builder.append(String.format("layout(location = %d) out %s %s;\n", attribute.location, attribute.type, attribute.name));
            }
            builder.append("\n");
        }
        else {
            //In
            for(Attribute attribute : this.vertOutAttributes.attributes) {
                builder.append(String.format("layout(location = %d) in %s %s;\n", attribute.location, attribute.type, attribute.name));
            }
            builder.append("\n");

            //TODO multi attachments?
            builder.append(String.format("layout(location = 0) out vec4 fragColor;\n\n"));
        }

        return builder.toString();
    }

    public void setShaderStage(GlslConverter.ShaderStage shaderStage) {
        this.shaderStage = shaderStage;
    }

    public record Attribute(int location, String type, String name) {}

    static class AttributeSet {
        List<Attribute> attributes = new ObjectArrayList<>();
        int currentLocation = 0;

        void add(String type, String name) {
            this.attributes.add(new Attribute(this.currentLocation, type, name));
            this.currentLocation++;
        }

        boolean contains(String type, String name) {
            return this.attributes.stream().anyMatch(attribute -> Objects.equals(attribute.name, name) && Objects.equals(attribute.type, type));
        }
    }
}
