#version 450
#include "fog.glsl"

layout(binding = 1) readonly uniform UBO{
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in float vertexDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = linear_fog(vertexColor, vertexDistance, FogStart, FogEnd, FogColor);
}