package net.vulkanmod.vulkan.shader.parser;

import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;

import java.util.*;

public class GlslConverter {
    
    ShaderStage shaderStage;
    private State state;

    private UniformParser uniformParser;
    private InputOutputParser inOutParser;

    private String vshConverted;
    private String fshConverted;

    public void process(String vertShader, String fragShader) {
        this.uniformParser = new UniformParser(this);
        this.inOutParser = new InputOutputParser(this);

        StringBuilder vshOut = this.processShaderFile(ShaderStage.Vertex, vertShader);
        vshOut.insert(0, this.inOutParser.createInOutCode());

        StringBuilder fshOut = this.processShaderFile(ShaderStage.Fragment, fragShader);
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

        this.vshConverted = vshOut.toString();
        this.fshConverted = fshOut.toString();

    }

    private StringBuilder processShaderFile(ShaderStage stage, String shader) {
        this.setShaderStage(stage);

        String[] lines = shader.split("\n");
        var out = new StringBuilder();

        var iterator = Arrays.stream(lines).iterator();

        while (iterator.hasNext()) {
            String line = iterator.next();

            int semicolons = charOccurences(line, ';');

            if (semicolons > 1) {
                var lines2 = line.splitWithDelimiters(";", 0);

                int matchingFor = 0;
                for (int i = 0; i < lines2.length;) {
                    StringBuilder line2 = new StringBuilder(lines2[i]);
                    i++;

                    matchingFor += charOccurences(line2.toString(), '(');
                    matchingFor -= charOccurences(line2.toString(), ')');

                    while (matchingFor > 0) {
                        String next = lines2[i];
                        i++;

                        matchingFor += charOccurences(next, '(');
                        matchingFor -= charOccurences(next, ')');

                        line2.append(next);
                    }

                    if (i < lines2.length) {
                        line2.append(lines2[i]);
                        i++;
                    }


                    if (matchingFor == 0) {
                        String parsedLine = this.parseLine(line2.toString());
                        if (parsedLine != null) {
                            out.append(parsedLine);
                            out.append("\n");
                        }
                    }

                }
            }
            else {
                String parsedLine = this.parseLine(line);
                if (parsedLine != null) {
                    out.append(parsedLine);
                    out.append("\n");
                }
            }

        }

        return out;
    }

    private int charOccurences(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }

        return count;
    }

    private String parseLine(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);

        // empty line
        if (!tokenizer.hasMoreTokens())
            return "\n";

        String token = tokenizer.nextToken();

        switch (token) {
            case "uniform" -> this.state = State.MATCHING_UNIFORM;
            case "in", "out" -> this.state = State.MATCHING_IN_OUT;
            case "#version" -> {
                return null;
            }
            case "#moj_import" -> {
                if (tokenizer.countTokens() != 1) {
                    throw new IllegalArgumentException("Token count != 1");
                }

                return String.format("#include %s", tokenizer.nextToken());
            }

            default -> {
                return CodeParser.parseCodeLine(line);
            }
        }

        if (tokenizer.countTokens() < 2) {
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

    public List<ImageDescriptor> getSamplerList() {
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
