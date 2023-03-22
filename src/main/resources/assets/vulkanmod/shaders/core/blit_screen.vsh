#version 450






const ivec2 tri[3] = ivec2[3](
    ivec2(1,1),
    ivec2(-3,1),
    ivec2(1,-3)
);



void main() {
    gl_Position = vec4(tri[gl_VertexIndex], 0.0, 1.0);


}
