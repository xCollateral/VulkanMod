#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in ivec2 UV2;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    mat4 ModelViewMat;
};

layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
layout(location = 2) out float vertexDistance;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

//    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
}

//#version 150
//
//#moj_import <fog.glsl>
//
//in vec3 Position;
//in vec4 Color;
//in ivec2 UV2;
//
//uniform sampler2D Sampler2;
//
//uniform mat4 ModelViewMat;
//uniform mat4 ProjMat;
//uniform mat3 IViewRotMat;
//uniform int FogShape;
//
//out float vertexDistance;
//out vec4 vertexColor;
//
//void main() {
//    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
//
////    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
//    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
//    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
//}
