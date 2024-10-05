#version 450

const vec4 pos[] = { vec4(-1, -1, 0, 1),  vec4(3, -1, 0, 1), vec4(-1, 3, 0, 1) };

#define INVERTED_UV

#ifndef INVERTED_UV
    const vec2 uv[] = { vec2(0, 1),  vec2(2, 1), vec2(0, -1) };
#else
    const vec2 uv[] = { vec2(0, 0),  vec2(2, 0), vec2(0, 2) };
#endif

layout(location = 0) out vec2 outUV;

void main() {
    outUV = uv[gl_VertexIndex];
    gl_Position = pos[gl_VertexIndex];
}