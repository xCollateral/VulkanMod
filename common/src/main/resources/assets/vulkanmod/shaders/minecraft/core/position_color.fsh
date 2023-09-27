#version 450

layout(location = 0) in vec4 vertexColor;

layout(binding = 1) uniform UBO{
    vec4 ColorModulator;
};

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
