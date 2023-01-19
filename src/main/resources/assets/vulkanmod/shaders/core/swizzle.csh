#version 460
#extension GL_ARB_separate_shader_objects : enable
const int WORKGROUP_SIZE = 1;
layout (local_size_x = 1024) in;


layout(std140, binding = 0) buffer image
{
   uvec4 imageData[];
};
const uint high8=(1<<32)-(1<<24);
void main()
{
	const uint xx=gl_GlobalInvocationID.x;
	//const uint yy=gl_GlobalInvocationID.y*WORKGROUP_SIZE;
	
	
	for(uint ii=0;ii<4;ii++)
	{
		uint R = (imageData[xx][ii]&255)<<16;
		uint G = (((imageData[xx][ii]&(65535))>>8)&255)<<8;
		uint B = ((imageData[xx][ii]&(16777215))>>16);
		uint A = (imageData[xx][ii]>>24<<24);
		imageData[xx][ii]=(R|G|B|A);
	}
	
	
	
	
}
