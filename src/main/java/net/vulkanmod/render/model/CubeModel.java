package net.vulkanmod.render.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

public class CubeModel {

    private ModelPart.Polygon[] polygons = new ModelPart.Polygon[6];
    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;

    Vector3f[] vertices;
    Vector3f[] transformed = new Vector3f[8];

    public void setVertices(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float q, float r, Set<Direction> set) {
        this.minX = f;
        this.minY = g;
        this.minZ = h;
        this.maxX = f + k;
        this.maxY = g + l;
        this.maxZ = h + m;
        this.polygons = new ModelPart.Polygon[set.size()];
        float s = maxX;
        float t = maxY;
        float u = maxZ;
        f -= n;
        g -= o;
        h -= p;
        s += n;
        t += o;
        u += p;
        if (bl) {
            float v = s;
            s = f;
            f = v;
        }

        this.vertices = new Vector3f[]{
                new Vector3f(f, g, h),
                new Vector3f(s, g, h),
                new Vector3f(s, t, h),
                new Vector3f(f, t, h),
                new Vector3f(f, g, u),
                new Vector3f(s, g, u),
                new Vector3f(s, t, u),
                new Vector3f(f, t, u)
        };

        for (int i1 = 0; i1 < 8; i1++) {
            //pre-divide all vertices once
            this.vertices[i1].div(16.0f);
            this.transformed[i1] = new Vector3f(0.0f);
        }

        ModelPart.Vertex vertex1 = new ModelPart.Vertex(transformed[0], 0.0F, 0.0F);
        ModelPart.Vertex vertex2 = new ModelPart.Vertex(transformed[1], 0.0F, 8.0F);
        ModelPart.Vertex vertex3 = new ModelPart.Vertex(transformed[2], 8.0F, 8.0F);
        ModelPart.Vertex vertex4 = new ModelPart.Vertex(transformed[3], 8.0F, 0.0F);
        ModelPart.Vertex vertex5 = new ModelPart.Vertex(transformed[4], 0.0F, 0.0F);
        ModelPart.Vertex vertex6 = new ModelPart.Vertex(transformed[5], 0.0F, 8.0F);
        ModelPart.Vertex vertex7 = new ModelPart.Vertex(transformed[6], 8.0F, 8.0F);
        ModelPart.Vertex vertex8 = new ModelPart.Vertex(transformed[7], 8.0F, 0.0F);

        float w = (float)i;
        float x = (float)i + m;
        float y = (float)i + m + k;
        float z = (float)i + m + k + k;
        float aa = (float)i + m + k + m;
        float ab = (float)i + m + k + m + k;
        float ac = (float)j;
        float ad = (float)j + m;
        float ae = (float)j + m + l;
        int idx = 0;
        if (set.contains(Direction.DOWN)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex6, vertex5, vertex1, vertex2}, x, ac, y, ad, q, r, bl, Direction.DOWN);
        }

        if (set.contains(Direction.UP)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex3, vertex4, vertex8, vertex7}, y, ad, z, ac, q, r, bl, Direction.UP);
        }

        if (set.contains(Direction.WEST)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex1, vertex5, vertex8, vertex4}, w, ad, x, ae, q, r, bl, Direction.WEST);
        }

        if (set.contains(Direction.NORTH)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex2, vertex1, vertex4, vertex3}, x, ad, y, ae, q, r, bl, Direction.NORTH);
        }

        if (set.contains(Direction.EAST)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex6, vertex2, vertex3, vertex7}, y, ad, aa, ae, q, r, bl, Direction.EAST);
        }

        if (set.contains(Direction.SOUTH)) {
            this.polygons[idx] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex5, vertex6, vertex7, vertex8}, aa, ad, ab, ae, q, r, bl, Direction.SOUTH);
        }
    }

    public void transformVertices(Matrix4f matrix) {
        //Transform original vertices and store them
        for(int i = 0; i < 8; ++i) {
            this.vertices[i].mulPosition(matrix, this.transformed[i]);
        }
    }

    public ModelPart.Polygon[] getPolygons() { return this.polygons; }
}
