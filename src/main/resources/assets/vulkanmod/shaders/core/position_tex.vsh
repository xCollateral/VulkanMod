#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in ivec2 UV0;

layout(Push_Constant) uniform UniformBufferObject {
   mat4 MVP;
};

layout(location = 0) out flat ivec2 texCoord0;

void main() {
    gl_Position = MVP * ivec4(Position, 1);

    texCoord0 = UV0;
}
