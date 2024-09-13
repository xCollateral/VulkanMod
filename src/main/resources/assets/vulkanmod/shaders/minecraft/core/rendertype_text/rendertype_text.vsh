#version 460

#include "fog.glsl"

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MatrixStack[32];
};

layout(binding = 2) uniform sampler2D Sampler2;

layout(location = 0) invariant flat out uint baseInstance;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec2 texCoord0;
layout(location = 3) out float vertexDistance;

void main() {
    gl_Position = MatrixStack[gl_BaseInstance & 31] * vec4(Position, 1.0);
    baseInstance = gl_BaseInstance>>16;

    vertexDistance = fog_distance(Position.xyz, 0);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
}
