#version 450

layout(push_constant) readonly uniform pushConstant {
    layout(offset = 32) vec4 ColorModulator;
};

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = ColorModulator;
}