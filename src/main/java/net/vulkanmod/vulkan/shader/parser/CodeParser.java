package net.vulkanmod.vulkan.shader.parser;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public abstract class CodeParser {

    /* TODO: this is not a proper parser, it just serves the purpose of converting glsl shaders
        to solve some simple and common compatibility issues.
        Implementing an AST would be a better solution.
     */
    public static String parseCodeLine(String line) {
        LinkedList<String> tokens = new LinkedList<>();
        StringTokenizer tokenizer = new StringTokenizer(line, " \t\n\r\f,(){}%", true);

        String delims = " \t\n\r\f";

        String token;
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();

            if (!delims.contains(token))
                tokens.add(token);
        }

        List<String> processed = new ObjectArrayList<>();
        boolean changed = false;

        int i = 0;
        while (i < tokens.size()) {
            token = tokens.get(i);

            if (token.equals("%")) {
                processed.removeLast();

                String prevToken = tokens.get(i - 1);
                String nextToken = tokens.get(i + 1);

                prevToken = checkTokenMapping(prevToken);
                nextToken = checkTokenMapping(nextToken);

                String newToken = "mod(%s, %s)".formatted(prevToken, nextToken);
                processed.add(newToken);

                changed = true;

                i += 2;
                continue;
            }

            String remappedToken = checkTokenMapping(token);

            if (!remappedToken.equals(token))
                changed = true;

            processed.add(remappedToken + " ");
            i++;
        }

        if (changed) {
            StringBuilder stringBuilder = new StringBuilder();

            for (String s : processed) {
                stringBuilder.append(s);
            }

            return stringBuilder.toString();
        } else {
            return line;
        }
    }

    private static String checkTokenMapping(String token) {
        return switch (token) {
            case "gl_VertexID" -> "gl_VertexIndex";
            default -> token;
        };
    }
}
