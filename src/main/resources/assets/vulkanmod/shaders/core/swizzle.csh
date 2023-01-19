#version 460

const int WORKGROUP_SIZE = 1;
layout (local_size_x = 32) in;

layout(Push_Constant) uniform pushConstant
{
	uvec2 ScreenSize;
};


layout(std140, binding = 0) buffer image
{
   uvec4 imageData[];
};
const uint high8=(1<<32)-(1<<24);
const uvec4 A = uvec4(255<<24);
//const uvec4 ARGB = uvec4(1,2,3,4)>>uvec4(1,2,3,4);
//const uvec4 RedMask = uvec4(255);
//const uvec4 BlueMask = uvec4(65535);
//const uvec4 GreenMask = uvec4(16777215);
//const uvec4 RedShift = uvec4(16);
//const uvec4 BlueShift = uvec4(8);
//const uvec4 GreenShift = uvec4(8);
void main()
{
	const uint xx=gl_GlobalInvocationID.x*32;
	//const uint yy=gl_GlobalInvocationID.y*WORKGROUP_SIZE;
	
	
	for(uint xOffs=xx;xOffs<xx+32;xOffs++)
	{
		
		{
			const uvec4 R = (imageData[xOffs]&255)<<16;
			const uvec4 G = (((imageData[xOffs]&(65535))>>8)&255)<<8;
			const uvec4 B = ((imageData[xOffs]&(16777215))>>16);
			
			imageData[xOffs]=(R|G|B|A);
		}
	}
	
	
	
	
}
