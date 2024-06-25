#version 450

#include "fog.glsl"

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 4) in ivec2 UV2;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    mat4 ModelViewMat;
    mat4 TextureMat;
    int FogShape;
};

layout(binding = 4) uniform sampler2D Sampler2;

layout(location = 0) out float vertexDistance;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec4 lightMapColor;
layout(location = 3) out vec2 texCoord0;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    vec4 pos = ModelViewMat * (Position, 1.0);
    vertexDistance = fog_distance(pos.xyz, FogShape);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    vertexColor = Color * lightMapColor;

    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
}