#version 450

#include "light.glsl"
#include "fog.glsl"

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    vec3 Light0_Direction;
    vec3 Light1_Direction;
};

layout(binding = 3) uniform sampler2D Sampler1;
layout(binding = 4) uniform sampler2D Sampler2;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec4 overlayColor;
layout(location = 2) out vec2 texCoord0;
layout(location = 3) out float vertexDistance;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    vertexDistance = fog_distance(Position.xyz, 0);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color) * texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
}