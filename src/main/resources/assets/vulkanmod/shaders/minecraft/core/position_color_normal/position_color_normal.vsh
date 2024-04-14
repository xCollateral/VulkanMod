#version 460

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec3 Normal;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP[64];
    mat4 ModelViewMat;
};

layout(location = 0) out float vertexDistance;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec4 normal;

void main() {
    gl_Position = MVP[gl_BaseInstance] * vec4(Position, 1.0);


    vertexColor = Color;
    //normal = MVP * vec4(Normal, 0.0);
}
