//light.glsl
//#pragma once
const float MINECRAFT_LIGHT_POWER  = (0.6);
const float MINECRAFT_AMBIENT_LIGHT = (0.4);

vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {
    lightDir0 = normalize(lightDir0);
    lightDir1 = normalize(lightDir1);
    float light0 = max(0.0, dot(lightDir0, normal));
    float light1 = max(0.0, dot(lightDir1, normal));
    float lightAccum = min(1.0, fma((light0 + light1), MINECRAFT_LIGHT_POWER, MINECRAFT_AMBIENT_LIGHT));
    return vec4(color.rgb * lightAccum, color.a);
}

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texelFetch(lightMap, bitfieldExtract(uv, 4, 8), 0); //return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

vec4 sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texelFetch(lightMap, bitfieldExtract(uv, 4, 8), 0);
}

vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    return (vertexDistance <= fogStart) ? inColor : mix(inColor, fogColor, min(smoothstep(fogStart, fogEnd, vertexDistance), 1.0) * fogColor.a);
}