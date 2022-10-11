#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV;
layout(location = 2) in vec4 Color;

layout(Push_Constant) uniform UniformBufferObject {
   mat4 MVP;
};

layout(location = 0) out vec2 texCoord;
layout(location = 1) out vec4 vertexColor;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    texCoord = UV;
    vertexColor = Color;
}
