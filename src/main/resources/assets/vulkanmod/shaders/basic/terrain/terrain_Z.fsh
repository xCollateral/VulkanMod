#version 450
layout(early_fragment_tests) in;
#include "light.glsl"

layout(binding = 3) uniform sampler2D Sampler0[];


layout(binding = 1) uniform InlineUniforms
{
    layout(offset=0) float FogStart;
    layout(offset=4) float FogEnd;
    layout(offset=16) vec4 FogColor;
};


layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 texCoord0;
//layout(location = 3) in vec4 normal;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0[3], texCoord0) * vertexColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
