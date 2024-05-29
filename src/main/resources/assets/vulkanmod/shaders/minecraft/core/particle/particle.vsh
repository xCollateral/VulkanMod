#version 460
layout (constant_id = 0) const bool USE_FOG = true;
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in ivec2 UV2;

layout(binding = 0) uniform readonly UniformBufferObject {
   mat4 MVP;
   layout(offset = 512) mat4 ModelViewMat;
};

layout(binding = 2) uniform sampler2D Sampler2;

layout(location = 0) invariant flat out uint baseInstance;
layout(location = 1) out vec2 texCoord0;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out float vertexDistance;

void main() {
    //TODO: Particles can share the same mat as terrain: Uniform indexing can be optimised out
    gl_Position = MVP * vec4(Position, 1.0);
    baseInstance = gl_BaseInstance >> 16;
    vertexDistance = USE_FOG ? length((ModelViewMat * vec4(Position, 1.0)).xyz) : 0.0f;
    texCoord0 = UV0;
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
}

/*
#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out float vertexDistance;
out vec2 texCoord0;
out vec4 vertexColor;
*/


