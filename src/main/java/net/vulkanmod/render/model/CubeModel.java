package net.vulkanmod.render.model;

import net.minecraft.client.model.geom.ModelPart.Polygon;
import java.util.HashSet;
import java.util.Set;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CubeModel {
    private static final float SCALE = 1/16F;

    private VertexPair[] vertices = new VertexPair[0];
    private VertexPair[] normals = new VertexPair[0];

    private boolean verticesCompiled;

    public void compileVertices(Polygon[] polygons) {
        if (verticesCompiled) {
            // only do it once:
            // We also only do this on-demand to account for instances where the vertices are altered/replaced.
            return;
        }

        // dedupe the vertices
        final Set<Vector3f> vertices = new HashSet<>();

        // note some custom cube implementations may have multiple faces with the same normal
        // so we treat them like vertices dedupe them all the same
        final Set<Vector3f> normals = new HashSet<>();

        for (Polygon polygon : polygons) {
            for (int i = 0; i < polygon.vertices.length; i++) {
                vertices.add(polygon.vertices[i].pos);
                normals.add(polygon.normal);
            }
        }

        // mul(1/16) as multiplication is faster, instruction-wise than division
        this.vertices = vertices.stream().map(vertex -> new VertexPair(new Vector3f(vertex.mul(SCALE)), vertex)).toArray(VertexPair[]::new);
        this.normals = normals.stream().map(normal -> new VertexPair(new Vector3f(normal), normal)).toArray(VertexPair[]::new);
        verticesCompiled = true;
    }

    public void transformVertices(Matrix4f positionMatrix, Matrix3f normalMatrix) {
        //Transform original vertices and store them
        for (VertexPair vertex : vertices) {
            vertex.original().mulPosition(positionMatrix, vertex.transformed());
        }
        for (VertexPair normal : normals) {
            normalMatrix.transform(normal.original(), normal.transformed());
        }
    }

    record VertexPair(Vector3f original, Vector3f transformed) {}
}
