#version 450
#extension GL_EXT_nonuniform_qualifier : enable
#extension GL_KHR_shader_subgroup_ballot : enable
vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    if (vertexDistance <= fogStart) {
        return inColor;
    }

    float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

layout(binding = 3) uniform sampler2D Sampler0[];

layout(binding = 1) uniform InlineUniforms
{
    layout(offset=0) float FogStart;
    layout(offset=4) float FogEnd;
    layout(offset=16) vec4 FogColor;
};

layout(location = 0) in flat uint baseInstance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 texCoord0;
layout(location = 3) in float vertexDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    const uint uniformBaseInstance = subgroupBroadcastFirst(baseInstance);
    vec4 color = texture(Sampler0[uniformBaseInstance], clamp(texCoord0, 0.0, 1.0));
    color *= vertexColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);;
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


