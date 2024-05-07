#version 450

#include "light.glsl"

layout(binding = 3) uniform sampler2D Sampler0[];

/*layout(binding = 1) uniform InlineUniforms
{
    float FogStart;
    float FogEnd;
    vec4 FogColor;
}*/


layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
//layout(location = 3) in vec4 normal;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0[3], texCoord0);
    if (color.a < 0.5f) {
        discard;
    }
    fragColor = color * vertexColor;
}
