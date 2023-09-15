package net.vulkanmod.vulkan.shader.parser;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.Image;
import net.vulkanmod.vulkan.shader.descriptor.UBO;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class GlslConverter {

//    private Queue<Integer> stack = new ArrayDeque<>();
    private int count;
    ShaderStage shaderStage;
    private State state;

    private UniformParser uniformParser;
    private InputOutputParser inOutParser;

    private String vshConverted;
    private String fshConverted;

    public void process(VertexFormat vertexFormat, String vertShader, String fragShader) {
        this.uniformParser = new UniformParser(this);
        this.inOutParser = new InputOutputParser(this, vertexFormat);

        StringBuilder vshOut = new StringBuilder();
        StringBuilder fshOut = new StringBuilder();

        this.setShaderStage(ShaderStage.Vertex);

        String[] lines = vertShader.split("\n");

        var iterator = Arrays.stream(lines).iterator();

        //TODO version
        while (iterator.hasNext()) {
            String line = iterator.next();

            String parsedLine = this.parseLine(line);
            if(parsedLine != null) {
                vshOut.append(parsedLine);
                vshOut.append("\n");
            }

        }

        vshOut.insert(0, this.inOutParser.createInOutCode());

        this.setShaderStage(ShaderStage.Fragment);

        lines = fragShader.split("\n");

        iterator = Arrays.stream(lines).iterator();

        while (iterator.hasNext()) {
            String line = iterator.next();

            String parsedLine = this.parseLine(line);
            if(parsedLine != null) {
                fshOut.append(parsedLine);
                fshOut.append("\n");
            }
        }

        fshOut.insert(0, this.inOutParser.createInOutCode());

        String uniformBlock = this.uniformParser.createUniformsCode();
        vshOut.insert(0, uniformBlock);
        fshOut.insert(0, uniformBlock);

        String samplersVertCode = this.uniformParser.createSamplersCode(ShaderStage.Vertex);
        String samplersFragCode = this.uniformParser.createSamplersCode(ShaderStage.Fragment);

        vshOut.insert(0, samplersVertCode);
        fshOut.insert(0, samplersFragCode);

        vshOut.insert(0, "#version 450\n\n");
        fshOut.insert(0, "#version 450\n\n");

        //TODO check
        //TODO ubo
        this.vshConverted = vshOut.toString();
        this.fshConverted = fshOut.toString();

    }

    private String parseLine(String line) {

        StringTokenizer tokenizer = new StringTokenizer(line);

        //empty line
        if(!tokenizer.hasMoreTokens()) return null;

        String token = tokenizer.nextToken();

        if(token.matches("uniform")) {
            this.state = State.MATCHING_UNIFORM;
        }
        else if(token.matches("in")) {
            this.state = State.MATCHING_IN_OUT;
        }
        else if(token.matches("out")) {
            this.state = State.MATCHING_IN_OUT;
        }
        else if(token.matches("#version")) {
            return null;
        }
        else {
            return line;
        }

        if(tokenizer.countTokens() < 2) {
            throw new IllegalArgumentException("Less than 3 tokens present");
        }

        feedToken(token);

        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();

            feedToken(token);
        }

        return null;
    }

    private void feedToken(String token) {
        switch (this.state) {
            case MATCHING_UNIFORM -> this.uniformParser.parseToken(token);
            case MATCHING_IN_OUT -> this.inOutParser.parseToken(token);
        }
    }

    private void setShaderStage(ShaderStage shaderStage) {
        this.shaderStage = shaderStage;
        this.uniformParser.setCurrentUniforms(this.shaderStage);
        this.inOutParser.setShaderStage(this.shaderStage);
    }

    public UBO getUBO() {
        return this.uniformParser.getUbo();
    }

    public List<Image> getSamplerList() {
        return this.uniformParser.getSamplers();
    }

    public String getVshConverted() {
        return vshConverted;
    }

    public String getFshConverted() {
        return fshConverted;
    }

    enum ShaderStage {
        Vertex,
        Fragment
    }

    enum State {
        MATCHING_UNIFORM,
        MATCHING_IN_OUT
    }
}
