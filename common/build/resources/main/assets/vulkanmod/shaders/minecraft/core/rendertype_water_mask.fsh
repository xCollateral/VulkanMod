#version 450

layout(binding = 1) uniform UniformBufferObject {
    vec4 ColorModulator;
};

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = ColorModulator;
}

/*
#version 150

uniform vec4 ColorModulator;

out vec4 fragColor;
*/

