#version 460
#pragma shader_stage(compute)

layout (local_size_x = 32) in;


layout(std430, binding = 0) restrict buffer Img
{
   uvec4 imageData[];
};

const uvec4 A = uvec4(255<<24);

void main()
{
	const uint xx=gl_GlobalInvocationID.x*32;
	//const uint yy=gl_GlobalInvocationID.y*WORKGROUP_SIZE;
	
	
	for(uint xOffs=xx;xOffs<xx+32;xOffs++)
	{
		
		{
			const uvec4 R = (imageData[xOffs]&255)<<16;
			const uvec4 G = (imageData[xOffs]&65535)>>8<<8;
			const uvec4 B = (imageData[xOffs]&16777215)>>16;
			
			imageData[xOffs]=(R|G|B|A);
		}
	}
	
	
	
	
}
