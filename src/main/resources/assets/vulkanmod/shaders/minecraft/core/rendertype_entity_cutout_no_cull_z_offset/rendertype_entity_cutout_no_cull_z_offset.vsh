#version 460

#include "light.glsl"

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP[64];
    mat4 ModelViewMat;
    vec3 Light0_Direction;
    vec3 Light1_Direction;
};

layout(binding = 2) uniform sampler2D Sampler2;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec4 lightMapColor;
layout(location = 2) out vec4 overlayColor;
layout(location = 3) out vec2 texCoord0;
layout(location = 4) out vec3 normal;
layout(location = 5) out float vertexDistance;

void main() {
    gl_Position = MVP[gl_BaseInstance] * vec4(Position, 1.0);


    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
//     //overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
    //normal = (MVP * vec4(Normal, 0.0)).xyz;
}