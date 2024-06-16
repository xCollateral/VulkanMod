#version 450
#include "fog.glsl"

layout(binding = 1) uniform UBO{
    vec4 ColorModulator;
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};

layout(location = 0) in float vertexDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = linear_fog(ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
