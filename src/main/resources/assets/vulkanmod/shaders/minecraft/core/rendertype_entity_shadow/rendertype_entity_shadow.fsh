#version 450

layout(binding = 3) uniform sampler2D Sampler0[];

layout(push_constant) readonly uniform pushConstant{
    layout(offset = 32) vec4 ColorModulator;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0[9], clamp(texCoord0, 0.0, 1.0));
    color *= vertexColor * ColorModulator;
    fragColor = color;
}