#version 460

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(binding = 0) uniform readonly UniformBufferObject {
   mat4 MatrixStack[32];
};

layout(location = 0) out vec4 vertexColor;

void main() {
    gl_Position = MatrixStack[gl_BaseInstance & 31] * vec4(Position, 1.0);

    vertexColor = Color;
}
