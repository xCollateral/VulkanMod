#version 450
layout (constant_id = 0) const bool USE_FOG = true;
layout(early_fragment_tests) in;
#include "light.glsl"

layout(binding = 2) uniform sampler2D Sampler0;

layout(binding = 1) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};


layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 texCoord0;
//layout(location = 3) in vec4 normal;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    fragColor = USE_FOG ? linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor) : color; //Optimised out by Driver
}
