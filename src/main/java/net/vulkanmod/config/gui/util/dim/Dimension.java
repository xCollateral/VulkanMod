package net.vulkanmod.config.gui.util.dim;

public interface Dimension<T extends Number> {
    static IntDimension ofInt(int x, int y, int width, int height) {
        return new IntDimension(x, y, width, height);
    }

    static FloatDimension ofFloat(float x, float y, float width, float height) {
        return new FloatDimension(x, y, width, height);
    }

    T x();

    T y();

    T width();

    T height();

    T xLimit();

    T yLimit();

    T centerX();

    T centerY();

    Dimension<T> setX(T x);

    Dimension<T> setY(T y);

    Dimension<T> setWidth(T width);

    Dimension<T> setHeight(T height);

    Dimension<T> copy();

    default boolean isPointInside(double x, double y) {
        return x >= x().doubleValue()
                && x < xLimit().doubleValue()
                && y >= y().doubleValue()
                && y < yLimit().doubleValue();
    }
}
