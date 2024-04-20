#version 450
layout(early_fragment_tests) in;
#include "light.glsl"

layout(binding = 3) uniform sampler2D Sampler0;



layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
//layout(location = 3) in vec4 normal;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    fragColor = color * vertexColor;
}
