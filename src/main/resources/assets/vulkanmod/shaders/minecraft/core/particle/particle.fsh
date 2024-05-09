#version 450
#extension GL_EXT_nonuniform_qualifier : enable
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
layout(location = 1) in vec2 texCoord0;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in float vertexDistance;


layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0[baseInstance], texCoord0) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }
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
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color;
}
*/
