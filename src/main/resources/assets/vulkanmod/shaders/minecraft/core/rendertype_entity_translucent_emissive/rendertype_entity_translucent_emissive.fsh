#version 450
#extension GL_EXT_nonuniform_qualifier : enable
#extension GL_KHR_shader_subgroup_ballot : enable
float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }

    return smoothstep(fogEnd, fogStart, vertexDistance);
}

layout(binding = 3) uniform sampler2D Sampler0[];

layout(push_constant) readonly uniform  PushConstant{
    layout(offset = 32) vec4 ColorModulator;
};

layout(binding = 1) uniform InlineUniforms
{
    layout(offset=0) float FogStart;
    layout(offset=4) float FogEnd;
    layout(offset=16) vec4 FogColor;
};


layout(location = 0) flat in uint baseInstance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec4 overlayColor;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in float vertexDistance;


layout(location = 0) out vec4 fragColor;

void main() {
    const uint uniformBaseInstance = subgroupBroadcastFirst(baseInstance);
    vec4 color = texture(Sampler0[uniformBaseInstance], texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    fragColor = color * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}

    /*
#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;
*/


