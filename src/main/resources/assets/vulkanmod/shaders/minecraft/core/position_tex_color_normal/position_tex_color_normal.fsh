#version 450

vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    if (vertexDistance <= fogStart) {
        return inColor;
    }

    float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

layout(binding = 3) uniform sampler2D Sampler0[];



layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
layout(location = 2) in float vertexDistance;
//layout(location = 3) in vec3 normal;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0[8], texCoord0) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color;
}


