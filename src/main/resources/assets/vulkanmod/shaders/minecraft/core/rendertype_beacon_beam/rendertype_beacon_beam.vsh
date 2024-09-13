#version 460

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;

layout(binding = 0) uniform readonly UniformBufferObject {
   mat4 MatrixStack[32];
};

layout(location = 0) out invariant flat uint baseInstance;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec2 texCoord0;
layout(location = 3) out invariant flat vec2 fragProj;

void main() {
    gl_Position = MatrixStack[gl_BaseInstance & 31] * vec4(Position, 1.0);
    baseInstance = gl_BaseInstance>>16;
    vertexColor = Color;
    texCoord0 = UV0;
    mat4 ProjMat =  MatrixStack[gl_BaseInstance + 1 & 31];
    fragProj = vec2(-ProjMat[3].z, ProjMat[2].z);
}