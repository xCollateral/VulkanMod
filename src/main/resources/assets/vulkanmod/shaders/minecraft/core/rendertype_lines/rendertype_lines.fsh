#version 450

layout(binding = 1) uniform UBO{
    vec4 ColorModulator;
};

layout(location = 0) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = vertexColor * ColorModulator;
    fragColor = color;
}