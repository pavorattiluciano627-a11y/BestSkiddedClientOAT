package dev.echo.client.ui;

/** Rectangle geometry and the original shared UiId-controlled hit-test branches. */
public record UiRect(float x, float y, float width, float height) {
    public UiRect {
        if (width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("A UI rectangle cannot have a negative extent");
        }
    }

    public float right() {
        return x + width;
    }

    public float bottom() {
        return y + height;
    }

    public boolean contains(float pointX, float pointY) {
        int guard = UiId.emptyCheckGuard();
        if (!(pointX >= x)) {
            return false;
        }
        if (guard != 0) {
            // Original FCMPG result is returned as boolean: unequal is true,
            // including an unordered right edge; no Y/height comparison occurs.
            return pointX != right();
        }
        return pointY >= y && pointX <= right() && pointY <= bottom();
    }

    public UiRect intersection(UiRect other) {
        float left = Math.max(x, other.x);
        float top = Math.max(y, other.y);
        float right = Math.min(right(), other.right());
        float bottom = Math.min(bottom(), other.bottom());
        return new UiRect(left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
    }
}
