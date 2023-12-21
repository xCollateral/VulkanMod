#version 450

layout(binding = 0) uniform sampler2D DiffuseSampler;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // blit final output of compositor into displayed back buffer
    fragColor = color;
}
