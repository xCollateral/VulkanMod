#version 460
#extension GL_ARB_separate_shader_objects : enable
const int WORKGROUP_SIZE = 1;
layout (local_size_x = 8) in;

layout(push_constant) uniform imageSize {
    vec2 ScreenSize;
};

layout(std140, binding = 0) buffer image
{
   uvec4 imageData[];
};

void main()
{
	const uint xx=gl_GlobalInvocationID.x*4;
	//const uint yy=gl_GlobalInvocationID.y*WORKGROUP_SIZE;
	
	for(uint i=gl_GlobalInvocationID.x;i<gl_GlobalInvocationID.x+4;i++)
	{
		imageData[i][0]=255U<<8|255U;
		imageData[i][1]=255U<<24|255U<<8|255U;
		imageData[i][2]=255U<<24|255U<<16|255U;
		imageData[i][3]=255U<<24|255U<<32|255U;
	}
	
	
}
