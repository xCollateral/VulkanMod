#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in ivec2 UV2;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP;
   mat4 ModelViewMat;
   vec4 ColorModulator;
};

layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out float vertexDistance;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
    vertexColor = Color * ColorModulator * texelFetch(Sampler2, UV2 / 16, 0);
}

/*
#version 150

in vec3 Position;
in vec4 Color;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 ColorModulator;

out float vertexDistance;
flat out vec4 vertexColor;
*/
