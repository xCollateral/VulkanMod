#version 450

const vec4 pos[] = { vec4(-1, -1, 0, 1),  vec4(3, -1, 0, 1), vec4(-1, 3, 0, 1) };
const vec2 uv[] = { vec2(0, 1),  vec2(2, 1), vec2(0, -1) };

layout(location = 0) out vec2 outUV;

void main() {
//    outUV = pos[gl_VertexIndex].xy+vec2(-1, 2); //This "ScreenCoord" UV is prob Wrong
    outUV = uv[gl_VertexIndex];
    gl_Position = pos[gl_VertexIndex];
}

//void main()
//{
//    vec2 screenPositions[] = vec2[](vec2(0.0f, 0.0f), vec2(1.0f, 0.0f), vec2(1.0f, 1.0f), vec2(0.0f, 1.0f));
//    vec2 ndcPositions[] = vec2[](vec2(-1.0f, -1.0f), vec2(1.0f, -1.0f), vec2(1.0f, 1.0f), vec2(-1.0f, 1.0f));
//    gl_Position.xy = ndcPositions[gl_VertexIndex];
//    gl_Position.z = 0.5f;
//    gl_Position.w = 1.0f;
//    vertScreenPos = screenPositions[gl_VertexIndex];
//}