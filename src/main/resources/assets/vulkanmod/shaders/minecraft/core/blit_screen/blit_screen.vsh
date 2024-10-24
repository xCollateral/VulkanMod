#version 450

layout(location = 0) in vec3 Position;
//layout(location = 1) in vec2 UV;



void main() {
    gl_Position = vec4(Position, 1.0);
}
