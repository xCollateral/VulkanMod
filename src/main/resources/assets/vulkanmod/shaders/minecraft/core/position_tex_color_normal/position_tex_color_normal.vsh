#version 460

#include "fog.glsl"

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MatrixStack[32];
};

layout(location = 0) out invariant flat uint baseInstance;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec2 texCoord0;
layout(location = 3) out float vertexDistance;

void main() {
    gl_Position = MatrixStack[gl_BaseInstance & 31] * vec4(Position, 1.0);
    baseInstance = gl_BaseInstance>>16;
    texCoord0 = UV0;
    vertexDistance = fog_distance(Position.xyz, 0);
    vertexColor = Color;
}


