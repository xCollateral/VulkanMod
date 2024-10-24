#version 450
layout (constant_id = 0) const float ALPHA_CUTOUT = 0.0f;
#include "light.glsl"
#include "fog.glsl"

layout(binding = 2) uniform sampler2D Sampler0;

layout(binding = 1) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};

layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    //Use a constexpr value to bypass a uniform load + improve alpha test performance + reduce memory access latency
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    //moving multiply after Alpha test seems to be more performant
    fragColor = linear_fog(color * vertexColor, vertexDistance, FogStart, FogEnd, FogColor);
}
