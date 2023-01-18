#version 460
#extension GL_ARB_separate_shader_objects : enable
const int WORKGROUP_SIZE = 32;
layout (local_size_x = WORKGROUP_SIZE, local_size_y = WORKGROUP_SIZE, local_size_z = 1 ) in;

layout(push_constant) uniform imageSize {
    vec2 ScreenSize;
};

layout(std140, binding = 0) buffer image
{
   uvec4 imageData[];
};

void main()
{
	const uint xx=gl_GlobalInvocationID.x*WORKGROUP_SIZE;
	const uint yy=gl_GlobalInvocationID.y*WORKGROUP_SIZE;
	
	for(uint x=xx; x< xx+WORKGROUP_SIZE; x++)
	{
		
		for(uint y=yy; y< yy+WORKGROUP_SIZE; y++)
		{
			imageData[x+y]=uvec4(0,0,0,255);
		}
		

	}
	
}
