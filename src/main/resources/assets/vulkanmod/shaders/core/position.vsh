#version 460

layout(location = 0) in vec3 Position;

layout(Push_Constant) uniform UniformBufferObject {
   mat4 MVP;
   mat4 ModelViewMat;
};

layout(location = 0) out float vertexDistance;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
}
