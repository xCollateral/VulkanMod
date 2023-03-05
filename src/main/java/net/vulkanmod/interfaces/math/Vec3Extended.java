package net.vulkanmod.interfaces.math;

import com.mojang.math.Vector3f;
import org.joml.Matrix4fc;

public interface Vec3Extended {

    Vector3f mulPosition(Matrix4fc mat, Vector3f dest);
}
