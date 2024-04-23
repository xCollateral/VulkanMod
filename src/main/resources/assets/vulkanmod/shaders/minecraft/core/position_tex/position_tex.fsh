#version 450
#extension GL_EXT_nonuniform_qualifier : enable
layout(binding = 3) uniform sampler2D Sampler0[];

layout(binding = 1) readonly uniform UBO{
    vec4 ColorModulator;
};

layout(location = 0) in flat uint baseInstance;
layout(location = 1) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0[baseInstance], texCoord0);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
