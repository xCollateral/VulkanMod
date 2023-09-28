#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP;
};

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
layout(location = 2) out vec2 texCoord1;
layout(location = 3) out vec2 texCoord2;
layout(location = 4) out vec4 normal;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;
    texCoord1 = UV1;
    texCoord2 = UV2;
    normal = MVP * vec4(Normal, 0.0);
}

/*
#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;
in vec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;
out vec4 normal;
*/

