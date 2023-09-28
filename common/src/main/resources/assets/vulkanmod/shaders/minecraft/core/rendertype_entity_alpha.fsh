#version 450

layout(binding = 1) uniform sampler2D Sampler0;

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
layout(location = 2) in vec2 texCoord1;
layout(location = 3) in vec2 texCoord2;
layout(location = 4) in vec4 normal;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < vertexColor.a) {
        discard;
    }
    fragColor = color;
}

/*
#version 150

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec2 texCoord2;
in vec4 normal;

out vec4 fragColor;
*/

