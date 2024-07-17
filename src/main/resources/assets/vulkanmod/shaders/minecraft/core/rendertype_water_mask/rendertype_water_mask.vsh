#version 460

layout(location = 0) in vec3 Position;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP[32];
};

void main() {
    gl_Position = MVP[gl_BaseInstance & 31] * vec4(Position, 1.0);
}