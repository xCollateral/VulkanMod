#version 460

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

layout(binding = 0) uniform readonly UniformBufferObject {
   mat4 MatrixStack[32];
};

layout(location = 0) invariant flat out uint baseInstance;
layout(location = 1) out vec2 texCoord0;

void main() {
    gl_Position = MatrixStack[gl_BaseInstance & 31] * vec4(Position, 1.0);
    baseInstance = gl_BaseInstance>>16;
    texCoord0 = UV0;
}
