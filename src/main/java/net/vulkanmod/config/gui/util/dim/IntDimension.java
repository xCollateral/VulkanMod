package net.vulkanmod.config.gui.util.dim;

public class IntDimension implements Dimension<Integer> {
    private int x, y;
    private int width, height;

    IntDimension(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public Integer x() {
        return x;
    }

    @Override
    public Integer y() {
        return y;
    }

    @Override
    public Integer width() {
        return width;
    }

    @Override
    public Integer height() {
        return height;
    }

    @Override
    public Integer xLimit() {
        return this.x + this.width;
    }

    @Override
    public Integer yLimit() {
        return this.y + this.height;
    }

    @Override
    public Integer centerX() {
        return this.x + (this.width / 2);
    }

    @Override
    public Integer centerY() {
        return this.y + (this.height / 2);
    }

    @Override
    public Dimension<Integer> setX(Integer x) {
        this.x = x;
        return this;
    }

    @Override
    public Dimension<Integer> setY(Integer y) {
        this.y = y;
        return this;
    }

    @Override
    public Dimension<Integer> setWidth(Integer width) {
        this.width = width;
        return this;
    }

    @Override
    public Dimension<Integer> setHeight(Integer height) {
        this.height = height;
        return this;
    }

    @Override
    public Dimension<Integer> copy() {
        return Dimension.ofInt(x, y, width, height);
    }
}
