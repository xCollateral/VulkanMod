#version 450

#include "fog.glsl"

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec3 Normal;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    mat4 ModelViewMat;
    int FogShape;
};

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
layout(location = 2) out float vertexDistance;
layout(location = 3) out vec3 normal;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    texCoord0 = UV0;
    vec4 pos = ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(pos.xyz, FogShape);
    vertexColor = Color;
}

//#version 150
//
//#moj_import <fog.glsl>
//
//in vec3 Position;
//in vec2 UV0;
//in vec4 Color;
//in vec3 Normal;
//
//uniform mat4 ModelViewMat;
//uniform mat4 ProjMat;
//uniform int FogShape;
//
//out vec2 texCoord0;
//out float vertexDistance;
//out vec4 vertexColor;
//
//void main() {
//    vec4 pos = ModelViewMat * vec4(Position, 1.0);
//    gl_Position = ProjMat * pos;
//
//    texCoord0 = UV0;
//    vertexDistance = fog_distance(pos.xyz, FogShape);
//    vertexColor = Color;
//}
