#version 450
#include "fog.glsl"

layout(binding = 1) uniform UBO{
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};

layout(push_constant) readonly uniform pushConstant{
    layout(offset = 32) vec4 ColorModulator;
};

layout(location = 0) in float vertexDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = linear_fog(ColorModulator, vertexDistance, 0, FogEnd, FogColor);
}
