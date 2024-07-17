#version 450
#extension GL_EXT_nonuniform_qualifier : enable
#extension GL_KHR_shader_subgroup_ballot : enable
#include "fog.glsl"



layout(binding = 1) uniform UBO{
    vec4 FogColor;
    float FogStart;
    float FogEnd;
};

layout(push_constant) readonly uniform pushConstant{
    layout(offset = 32) vec4 ColorModulator;
};

layout(binding = 3) uniform sampler2D Sampler0[];

layout(location = 0) in flat uint baseInstance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 texCoord0;
layout(location = 3) in flat vec2 fragProj; //Workaround to allow binding 0 to be vertex only (Access flags optimization)

layout(location = 0) out vec4 fragColor;
//TODO: Vanilla bug: Fog is broken with Beacon beams
void main() {
    const uint uniformBaseInstance = subgroupBroadcastFirst(baseInstance);
    vec4 color = texture(Sampler0[uniformBaseInstance], texCoord0);
    color *= vertexColor * ColorModulator;
    float fragmentDistance = fragProj.x / ((gl_FragCoord.z) * -2.0 + 1.0 - fragProj.y);
    fragColor = linear_fog(color, fragmentDistance, FogStart, FogEnd, FogColor);
}