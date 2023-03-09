package net.vulkanmod.render.chunk;

import org.joml.FrustumIntersection;
import org.joml.Vector3i;

import javax.annotation.Nullable;

import static net.vulkanmod.render.chunk.ChunkAreaManager.*;

public class ChunkArea {

    private int inFrustum;

    public final Vector3i position;
    private final int width;
    private final int height;
    private final int depth;

    final ChunkArea parent;
    final ChunkArea[] children;
    int childPos = 0;
    int level;

    //debug
    int sectionPos = 0;
    public final RenderSection[] sections;

    public void addSection(RenderSection section) {
        if(this.sectionPos == 8)
            throw new RuntimeException("beyond range");
        this.sections[sectionPos] = section;
        this.sectionPos++;
    }

    public void setSection(RenderSection section, int idx) {
        this.sections[idx] = section;
    }

    public void unbindSection(RenderSection section) {
        //TODO later find a better way than linear scan
        section.releaseBuffers();
        for(int i = 0; i < this.sections.length; ++i) {
            if(this.sections[i] == section) {
                this.sections[i] = null;
                return;
            }
        }
    }

    public boolean hasNoSection() {
        for(RenderSection section1 : this.sections) {
            if(section1 != null) return false;
        }
        return true;
    }

    public ChunkArea(Vector3i position, int level, @Nullable ChunkArea parent) {
        this.position = position;

        this.level = level;

        this.width = ChunkAreaManager.WIDTH >> level;
        this.height = ChunkAreaManager.HEIGHT >> level;
        this.depth = ChunkAreaManager.DEPTH >> level;

        this.parent = parent;

        if(level == ChunkAreaManager.LEVELS - 1) {
            this.children = null;
            this.sections = new RenderSection[ChunkAreaManager.LOWEST_LVL_SECTIONS];
        } else {
            this.children = new ChunkArea[8];
            this.sections = null;
        }

//        if(parent != null) {
//            parent.addChild(this);
//        }

    }

    public void updateFrustum(VFrustum frustum) {
//        BlockPos pos = viewArea.chunks[this.startIdx].getOrigin();
//        int frustumResult = frustum.cubeInFrustum(pos.getX(), pos.getY(), pos.getZ(),
//                pos.getX() + (this.width << 4) , pos.getY() + (this.height << 4), pos.getZ() + (this.depth << 4));

        int frustumResult = frustum.cubeInFrustum(this.position.x(), this.position.y(), this.position.z(),
                this.position.x() + (this.width << 4) , this.position.y() + (this.height << 4), this.position.z() + (this.depth << 4));

        if (frustumResult == FrustumIntersection.INTERSECT) {
            if(this.children != null) {
                for(ChunkArea child : this.children) {
                    if(child == null) continue;
                    child.updateFrustum(frustum);
                }
            }
        } else {
            this.setFrustum(frustumResult);
        }

//        if(children != null) {
//            for(ChunkArea child : children) {
//                if(child == null) continue;
//                child.setFrustum(frustumResult);
//            }
//        }

        this.inFrustum = frustumResult;
    }

    private Vector3i getBlockPosition() {
        return new Vector3i(this.position).mul(ChunkAreaManager.WIDTH >> this.level);
    }

    public int inFrustum() {
        return this.inFrustum;
    }

    private void setFrustum(int frustumResult) {
        if(this.children != null) {
            for(ChunkArea child : this.children) {
                if(child == null) continue;
                child.setFrustum(frustumResult);
            }
        }
        this.inFrustum = frustumResult;
    }

//    public void setChildren(ChunkArea[] children) {
//        this.children = children;
//    }

    public boolean hasNoChildren() {
        for(ChunkArea child : this.children) {
            if(child != null) return false;
        }
        return true;
    }

    public void addChild(ChunkArea child) {
        if(this.childPos == 8) throw new IndexOutOfBoundsException();
        this.children[childPos] = child;
        this.childPos++;
    }

    public void setChild(ChunkArea child, int idx) {
        this.children[idx] = child;
    }

    public void removeChild(ChunkArea child) {
        Vector3i vec3 = child.position;

        int shX = BASE_SH_X - child.level + 4;
        int shY = BASE_SH_Y - child.level + 4;
        int shZ = BASE_SH_Z - child.level + 4;

        int AreaX = vec3.x >> shX;
        int AreaY = vec3.y >> shY;
        int AreaZ = vec3.z >> shZ;

        int x1 = AreaX - (AreaX >> 1 << 1);
        int y1 = AreaY - (AreaY >> 1 << 1);
        int z1 = AreaZ - (AreaZ >> 1 << 1);

        int idx = 2 * (y1 * 2 + z1) + x1;

        this.children[idx] = null;
    }
}
