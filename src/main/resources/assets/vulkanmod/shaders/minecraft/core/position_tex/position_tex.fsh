#version 450

layout(binding = 3) uniform sampler2D Sampler0;


layout(location = 0) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color;
}
