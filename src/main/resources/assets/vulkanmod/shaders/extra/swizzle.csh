#version 460
#pragma shader_stage(compute)

layout (local_size_x = 32, local_size_y = 32) in;


layout(binding = 0, rgba8ui) uniform restrict uimage2D Img;

//const uint defSize=1u;
const uvec4 A = uvec4(255<<24);

void main()
{
	const uint xx = (gl_GlobalInvocationID.x);
	const uint yy = (gl_GlobalInvocationID.y);
	//const uint yy=gl_GlobalInvocationID.y*WORKGROUP_SIZE;
	
	
	//for(int xOffs=xx;xOffs<xx+defSize;xOffs++)
	{
		
		//for(int yOffs=yy; yOffs<yy+defSize; yOffs++)
		{
		
			
			imageStore(Img, ivec2(xx, yy), imageLoad(Img, ivec2(xx, yy)).bgra);
		}
	}
	
	
	
	
}
