#version 450

layout(binding = 2) uniform sampler2D DiffuseSampler;

layout(binding = 1) uniform UBO {
    vec4 ColorModulator;
};

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord) * vertexColor;

    // blit final output of compositor into displayed back buffer
    fragColor = color * ColorModulator;
}
