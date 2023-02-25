#version 460
#pragma shader_stage(compute)

layout (local_size_x = 32, local_size_y = 32) in;


layout(binding = 0, rgba8) uniform restrict image2D Img;

//const uint defSize=1u;
const uint A = 255u;

void main()
{
	const ivec2 xy = ivec2(gl_GlobalInvocationID.x, gl_GlobalInvocationID.y);
	
	
	
	//for(int xOffs=xx;xOffs<xx+defSize;xOffs++)
	{
		
		//for(int yOffs=yy; yOffs<yy+defSize; yOffs++)
		{
		
			
			imageStore(Img, xy, vec4(imageLoad(Img, xy).bgr, A));
		}
	}
	
	
	
	
}
