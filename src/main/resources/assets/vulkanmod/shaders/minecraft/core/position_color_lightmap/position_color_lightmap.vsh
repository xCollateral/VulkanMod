#version 460

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV2;

layout(binding = 0) uniform readonly UniformBufferObject {
    mat4 MVP[8];
};

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord2;

void main() {
    gl_Position = MVP[gl_BaseInstance] * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord2 = UV2;
}
