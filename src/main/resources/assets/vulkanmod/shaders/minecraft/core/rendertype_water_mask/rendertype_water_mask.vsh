#version 450

layout(location = 0) in vec3 Position;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP;
};

void main() {
    gl_Position = MVP * vec4(Position, 1.0);
}