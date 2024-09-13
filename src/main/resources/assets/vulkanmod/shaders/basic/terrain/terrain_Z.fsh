#version 450

layout(early_fragment_tests) in;
#include "light.glsl"
#include "fog.glsl"

layout(binding = 3, set = SET_ID) uniform sampler2D Sampler0;

layout(binding = 1, set = SET_ID) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};


layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
