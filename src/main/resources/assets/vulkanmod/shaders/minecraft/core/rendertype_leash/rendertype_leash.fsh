#version 450
#include "fog.glsl"

layout(binding = 1) uniform UBO{
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in float vertexDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = linear_fog(vertexColor, vertexDistance, FogStart, FogEnd, FogColor);
}

/*
#version 150

#moj_import <fog.glsl>

uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
flat in vec4 vertexColor;

out vec4 fragColor;

void main() {
    fragColor = linear_fog(vertexColor, vertexDistance, FogStart, FogEnd, FogColor);
}
*/
