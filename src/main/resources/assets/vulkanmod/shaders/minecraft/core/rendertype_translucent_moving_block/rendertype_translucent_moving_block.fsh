#version 450

layout(binding = 2) uniform sampler2D Sampler0;
layout(binding = 3) uniform sampler2D Sampler2;

layout(binding = 1) uniform UBO{
    vec4 ColorModulator;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    fragColor = color * ColorModulator;
}

/*
#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;
*/

