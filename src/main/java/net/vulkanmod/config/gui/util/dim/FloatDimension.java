package net.vulkanmod.config.gui.util.dim;

public class FloatDimension implements Dimension<Float> {
    private float x, y;
    private float width, height;

    FloatDimension(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public Float x() {
        return x;
    }

    @Override
    public Float y() {
        return y;
    }

    @Override
    public Float width() {
        return width;
    }

    @Override
    public Float height() {
        return height;
    }

    @Override
    public Float xLimit() {
        return this.x + this.width;
    }

    @Override
    public Float yLimit() {
        return this.y + this.height;
    }

    @Override
    public Float centerX() {
        return this.x + (this.width / 2);
    }

    @Override
    public Float centerY() {
        return this.y + (this.height / 2);
    }

    @Override
    public Dimension<Float> setX(Float x) {
        this.x = x;
        return this;
    }

    @Override
    public Dimension<Float> setY(Float y) {
        this.y = y;
        return this;
    }

    @Override
    public Dimension<Float> setWidth(Float width) {
        this.width = width;
        return this;
    }

    @Override
    public Dimension<Float> setHeight(Float height) {
        this.height = height;
        return this;
    }

    @Override
    public Dimension<Float> copy() {
        return Dimension.ofFloat(x, y, width, height);
    }
}
