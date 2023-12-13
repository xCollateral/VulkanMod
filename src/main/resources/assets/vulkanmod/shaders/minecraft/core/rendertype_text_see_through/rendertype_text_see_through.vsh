#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP;
};

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;
}

