#version 460

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;

layout(binding = 0) uniform readonly UniformBufferObject {
    mat4 MVP[8];
};

layout(location = 0) invariant flat out uint baseInstance;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec2 texCoord0;

void main() {
    gl_Position = MVP[gl_BaseInstance& 7] * vec4(Position, 1.0);
    baseInstance = gl_BaseInstance>>16;
    vertexColor = Color;
    texCoord0 = UV0;
}
