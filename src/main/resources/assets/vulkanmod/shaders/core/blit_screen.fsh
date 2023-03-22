#version 450






layout(location = 0) out vec4 fragColor;

layout(push_constant) uniform pushConstant {
    vec3 clearColor;
};

void main() {
   
    // blit final output of compositor into displayed back buffer
    fragColor = vec4(clearColor, 0);
}
