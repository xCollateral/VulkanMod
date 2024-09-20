package net.vulkanmod.render.sky;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;

import java.io.IOException;

public class CloudRenderer {
    // TODO move to util
    private static final int DIR_NEG_Y_BIT = 1 << 0;
    private static final int DIR_POS_Y_BIT = 1 << 1;
    private static final int DIR_NEG_X_BIT = 1 << 2;
    private static final int DIR_POS_X_BIT = 1 << 3;
    private static final int DIR_NEG_Z_BIT = 1 << 4;
    private static final int DIR_POS_Z_BIT = 1 << 5;

    private static final int CELL_WIDTH = 12;
    private static final int CELL_HEIGHT = 4;

    private CloudGrid cloudGrid;

    private int prevCloudX;
    private int prevCloudY;
    private int prevCloudZ;

    private CloudStatus prevCloudsType;
    private boolean prevInsideClouds;

    private boolean generateClouds;
    private VertexBuffer cloudBuffer;

    public CloudRenderer(ResourceLocation textureLocation) {
        loadTexture(textureLocation);
    }

    public void loadTexture(ResourceLocation location) {
        this.cloudGrid = createCloudGrid(location);
    }

    public void renderClouds(ClientLevel level, PoseStack poseStack, Matrix4f modelView, Matrix4f projection, float ticks, float partialTicks, double camX, double camY, double camZ) {
        float cloudHeight = level.effects().getCloudHeight();

        if (Float.isNaN(cloudHeight)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        double timeOffset = (ticks + partialTicks) * 0.03F;
        double centerX = (camX + timeOffset);
        double centerZ = camZ + 0.33F * CELL_WIDTH;
        double centerY = cloudHeight - (float) camY + 0.33F;

        int centerCellX = (int) Math.floor(centerX / CELL_WIDTH);
        int centerCellZ = (int) Math.floor(centerZ / CELL_WIDTH);

        boolean insideClouds = (centerY >= -4.0f && centerY <= 0.0f);

        if (centerCellX != this.prevCloudX || centerCellZ != this.prevCloudZ
                || (minecraft.options.getCloudsType() != this.prevCloudsType)
                || (this.prevInsideClouds != insideClouds)
                || this.cloudBuffer == null) {
            this.prevCloudX = centerCellX;
            this.prevCloudZ = centerCellZ;
            this.prevCloudsType = minecraft.options.getCloudsType();
            this.prevInsideClouds = insideClouds;
            this.generateClouds = true;
        }

        if (this.generateClouds) {
            this.generateClouds = false;
            if (this.cloudBuffer != null) {
                this.cloudBuffer.close();
            }

            this.cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            this.cloudBuffer.bind();

            MeshData cloudsMesh = this.buildClouds(Tesselator.getInstance(), centerCellX, centerCellZ, centerY);
            this.cloudBuffer.upload(cloudsMesh);
            VertexBuffer.unbind();
        }

        FogRenderer.levelFogColor();

        float xTranslation = (float)(centerX - (centerCellX * CELL_WIDTH));
        float yTranslation = (float) centerY;
        float zTranslation = (float)(centerZ - (centerCellZ * CELL_WIDTH));

        poseStack.pushPose();
        poseStack.mulPose(modelView);
        poseStack.translate(-xTranslation, yTranslation, -zTranslation);

        Vec3 cloudColor = level.getCloudColor(partialTicks);
        RenderSystem.setShaderColor((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 0.8f);

        this.cloudBuffer.bind();

        ShaderInstance shaderInstance = GameRenderer.getPositionColorShader();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        boolean fastClouds = this.prevCloudsType == CloudStatus.FAST;
        boolean disableCull = insideClouds || (fastClouds && centerY <= 0.0f);

        if (disableCull) {
            RenderSystem.disableCull();
        }

        if (!fastClouds) {
            RenderSystem.colorMask(false, false, false, false);
            this.cloudBuffer.drawWithShader(poseStack.last().pose(), projection, shaderInstance);

            RenderSystem.colorMask(true, true, true, true);
        }

        this.cloudBuffer.drawWithShader(poseStack.last().pose(), projection, shaderInstance);

        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        VertexBuffer.unbind();

        poseStack.popPose();
    }

    public void reset() {
        if (this.cloudBuffer != null) {
            this.cloudBuffer.close();
            this.cloudBuffer = null;
        }
    }

    private MeshData buildClouds(Tesselator tesselator, int centerCellX, int centerCellZ, double cloudY) {

        final int upFaceColor = ColorUtil.RGBA.pack(1.0f, 1.0f, 1.0f, 1.0f);
        final int xDirColor = ColorUtil.RGBA.pack(0.9f, 0.9f, 0.9f, 1.0f);
        final int downFaceColor = ColorUtil.RGBA.pack(0.7f, 0.7f, 0.7f, 1.0f);
        final int zDirColor = ColorUtil.RGBA.pack(0.8f, 0.8f, 0.8f, 1.0f);

        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int renderDistance = 32;
        boolean insideClouds = this.prevInsideClouds;

        if (this.prevCloudsType == CloudStatus.FANCY) {

            for (int cellX = -renderDistance; cellX < renderDistance; ++cellX) {
                for (int cellZ = -renderDistance; cellZ < renderDistance; ++cellZ) {
                    int cellIdx = this.cloudGrid.getWrappedIdx(centerCellX + cellX, centerCellZ + cellZ);
                    byte renderFaces = this.cloudGrid.renderFaces[cellIdx];

                    float x = cellX * CELL_WIDTH;
                    float z = cellZ * CELL_WIDTH;

                    if ((renderFaces & DIR_POS_Y_BIT) != 0 && cloudY <= 0.0f) {
                        putVertex(bufferBuilder, x + CELL_WIDTH, CELL_HEIGHT, z + CELL_WIDTH, upFaceColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, CELL_HEIGHT, z + 0.0f, upFaceColor);
                        putVertex(bufferBuilder, x + 0.0f, CELL_HEIGHT, z + 0.0f, upFaceColor);
                        putVertex(bufferBuilder, x + 0.0f, CELL_HEIGHT, z + CELL_WIDTH, upFaceColor);
                    }

                    if ((renderFaces & DIR_NEG_Y_BIT) != 0 && cloudY >= -CELL_HEIGHT) {
                        putVertex(bufferBuilder, x + 0.0f, 0.0f, z + CELL_WIDTH, downFaceColor);
                        putVertex(bufferBuilder, x + 0.0f, 0.0f, z + 0.0f, downFaceColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, 0.0f, z + 0.0f, downFaceColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, 0.0f, z + CELL_WIDTH, downFaceColor);
                    }

                    if ((renderFaces & DIR_POS_X_BIT) != 0 && (x < 1.0f || insideClouds)) {
                        putVertex(bufferBuilder, x + CELL_WIDTH, CELL_HEIGHT, z + CELL_WIDTH, xDirColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, 0.0f, z + CELL_WIDTH, xDirColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, 0.0f, z + 0.0f, xDirColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, CELL_HEIGHT, z + 0.0f, xDirColor);
                    }

                    if ((renderFaces & DIR_NEG_X_BIT) != 0 && (x > -1.0f || insideClouds)) {
                        putVertex(bufferBuilder, x + 0.0f, CELL_HEIGHT, z + 0.0f, xDirColor);
                        putVertex(bufferBuilder, x + 0.0f, 0.0f, z + 0.0f, xDirColor);
                        putVertex(bufferBuilder, x + 0.0f, 0.0f, z + CELL_WIDTH, xDirColor);
                        putVertex(bufferBuilder, x + 0.0f, CELL_HEIGHT, z + CELL_WIDTH, xDirColor);
                    }

                    if ((renderFaces & DIR_POS_Z_BIT) != 0 && (z < 1.0f || insideClouds)) {
                        putVertex(bufferBuilder, x + 0.0f, CELL_HEIGHT, z + CELL_WIDTH, zDirColor);
                        putVertex(bufferBuilder, x + 0.0f, 0.0f, z + CELL_WIDTH, zDirColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, 0.0f, z + CELL_WIDTH, zDirColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, CELL_HEIGHT, z + CELL_WIDTH, zDirColor);
                    }

                    if ((renderFaces & DIR_NEG_Z_BIT) != 0 && (z > -1.0f || insideClouds)) {
                        putVertex(bufferBuilder, x + CELL_WIDTH, CELL_HEIGHT, z + 0.0f, zDirColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, 0.0f, z + 0.0f, zDirColor);
                        putVertex(bufferBuilder, x + 0.0f, 0.0f, z + 0.0f, zDirColor);
                        putVertex(bufferBuilder, x + 0.0f, CELL_HEIGHT, z + 0.0f, zDirColor);
                    }

                }
            }
        } else {

            for (int cellX = -renderDistance; cellX < renderDistance; ++cellX) {
                for (int cellZ = -renderDistance; cellZ < renderDistance; ++cellZ) {
                    int cellIdx = this.cloudGrid.getWrappedIdx(centerCellX + cellX, centerCellZ + cellZ);
                    byte renderFaces = this.cloudGrid.renderFaces[cellIdx];

                    float x = cellX * CELL_WIDTH;
                    float z = cellZ * CELL_WIDTH;

                    if ((renderFaces & DIR_NEG_Y_BIT) != 0) {
                        putVertex(bufferBuilder, x + 0.0f, 0.0f, z + CELL_WIDTH, upFaceColor);
                        putVertex(bufferBuilder, x + 0.0f, 0.0f, z + 0.0f, upFaceColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, 0.0f, z + 0.0f, upFaceColor);
                        putVertex(bufferBuilder, x + CELL_WIDTH, 0.0f, z + CELL_WIDTH, upFaceColor);
                    }

                }
            }
        }

        return bufferBuilder.buildOrThrow();
    }

    private static void putVertex(BufferBuilder bufferBuilder, float x, float y, float z, int color) {
        bufferBuilder.addVertex(x, y, z).setColor(color);
    }

    private static CloudGrid createCloudGrid(ResourceLocation textureLocation) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        try {
            Resource resource = resourceManager.getResourceOrThrow(textureLocation);

            try (var inputStream = resource.open()) {
                NativeImage image = NativeImage.read(inputStream);

                int width = image.getWidth();
                int height = image.getHeight();
                Validate.isTrue(width == height, "Image width and height must be the same");

                int[] pixels = image.getPixelsRGBA();

                return new CloudGrid(pixels, width);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class CloudGrid {
        final int width;
        final int[] pixels;
        final byte[] renderFaces;

        CloudGrid(int[] pixels, int width) {
            this.pixels = pixels;
            this.width = width;

            this.renderFaces = computeRenderFaces();
        }

        byte[] computeRenderFaces() {
            byte[] renderFaces = new byte[pixels.length];

            for (int z = 0; z < this.width; z++) {
                for (int x = 0; x < this.width; x++) {
                    int idx = this.getIdx(x, z);
                    int pixel = this.pixels[idx];

                    if (!hasColor(pixel)) {
                        continue;
                    }

                    byte faces = DIR_NEG_Y_BIT | DIR_POS_Y_BIT;

                    int adjPixel;

                    adjPixel = getTexelWrapped(x - 1, z);
                    if (pixel != adjPixel) {
                        faces |= DIR_NEG_X_BIT;
                    }

                    adjPixel = getTexelWrapped(x + 1, z);
                    if (pixel != adjPixel) {
                        faces |= DIR_POS_X_BIT;
                    }

                    adjPixel = getTexelWrapped(x, z - 1);
                    if (pixel != adjPixel) {
                        faces |= DIR_NEG_Z_BIT;
                    }

                    adjPixel = getTexelWrapped(x, z + 1);
                    if (pixel != adjPixel) {
                        faces |= DIR_POS_Z_BIT;
                    }

                    renderFaces[idx] = faces;
                }
            }

            return renderFaces;
        }

        int getTexelWrapped(int x, int z) {
            if (x < 0) {
                x = this.width - 1;
            }

            if (x > this.width - 1) {
                x = 0;
            }

            if (z < 0) {
                z = this.width - 1;
            }

            if (z > this.width - 1) {
                z = 0;
            }

            return this.pixels[getIdx(x, z)];
        }

        int getWrappedIdx(int x, int z) {
            x = Math.floorMod(x, this.width);
            z = Math.floorMod(z, this.width);

            return this.getIdx(x, z);
        }

        int getIdx(int x, int z) {
            return z * this.width + x;
        }

        private static boolean hasColor(int pixel) {
            return ((pixel >> 24) & 0xFF) > 1;
        }
    }
}
