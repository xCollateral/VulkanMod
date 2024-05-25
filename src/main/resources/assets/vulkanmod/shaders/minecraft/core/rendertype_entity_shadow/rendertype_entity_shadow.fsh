#version 450
#include "fog.glsl"

layout(binding = 2) uniform sampler2D Sampler0;

layout(binding = 1) uniform UBO{
    vec4 ColorModulator;
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
layout(location = 2) in float vertexDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, clamp(texCoord0, 0.0, 1.0));
    color *= vertexColor * ColorModulator;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

/*
#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;
*/


