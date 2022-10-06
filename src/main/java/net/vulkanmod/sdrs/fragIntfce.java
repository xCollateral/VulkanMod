package net.vulkanmod.sdrs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.system.MemoryUtil;
public class  fragIntfce
{
 public static int currentSize;
	private static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
public static long getFunc(String aa)
 { 
	 int[]ax; 
try {
		ax= (int[]) Class.forName("net.vulkanmod.sdrs.frag"+aa, false, systemClassLoader).getField("frag"+aa).get(int[].class);
	 } catch (ReflectiveOperationException e) {
		 throw new RuntimeException(e);
	 };ByteBuffer axl = MemoryUtil.memAlignedAlloc(Integer.SIZE, ax.length*4); //Align to native uint32_t which is the alignment for the vkShaderModuleCreateInfo pCode in the C/C++ API
  axl.asIntBuffer().put(ax);
  currentSize=axl.remaining();
return MemoryUtil.memAddress0(axl);
}
}