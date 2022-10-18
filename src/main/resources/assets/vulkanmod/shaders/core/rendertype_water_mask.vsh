#version 460

layout(location = 0) in vec3 Position;

layout(Push_Constant) uniform UniformBufferObject {
   mat4 MVP;
};

void main() {
    gl_Position = MVP * vec4(Position, 1.0);
}

/*
#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);
}
*/
