#version 450
#include "fog.glsl"

layout(binding = 1) uniform UBO{
    vec4 ColorModulator;
    float FogStart;
    float FogEnd;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in float vertexDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vertexColor * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}

/*
#version 150

#moj_import <fog.glsl>

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;


*/
