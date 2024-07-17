//#version 150
//
//#moj_import <fog.glsl>
//
//uniform vec4 ColorModulator;
//uniform float FogStart;
//uniform float FogEnd;
//uniform vec4 FogColor;
//
//in float vertexDistance;
//in vec4 vertexColor;
//in vec4 normal;
//
//out vec4 fragColor;
//
//void main() {
//    vec4 color = vertexColor * ColorModulator;
//    if (color.a < 0.1) {
//        discard;
//    }
//    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
//}

#version 450

#include <fog.glsl>

layout(binding = 3) uniform sampler2D Sampler0;

layout(binding = 1) uniform UBO{
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};

layout(push_constant) readonly uniform pushConstant{
    layout(offset = 32) vec4 ColorModulator;
};

layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}