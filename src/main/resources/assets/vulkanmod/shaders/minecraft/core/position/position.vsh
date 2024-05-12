#version 460

layout(location = 0) in vec3 Position;

layout(binding = 0) uniform readonly UniformBufferObject {
   mat4 MVP[8];
   layout(offset = 512) mat4 ModelViewMat[4];
};

layout(location = 0) out float vertexDistance;

void main() {
    gl_Position = MVP[gl_BaseInstance & 7] * vec4(Position, 1.0);

    vertexDistance = length((ModelViewMat[(gl_BaseInstance & 127) >> 5] * vec4(Position, 1.0)).xyz);
}
