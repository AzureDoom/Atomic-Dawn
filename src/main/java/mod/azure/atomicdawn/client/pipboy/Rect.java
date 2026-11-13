package mod.azure.atomicdawn.client.pipboy;

public record Rect(
    int x,
    int y,
    int width,
    int height
) {

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public Rect inset(int amount) {
        return new Rect(x + amount, y + amount, Math.max(0, width - amount * 2), Math.max(0, height - amount * 2));
    }

    public Rect left(int w) {
        return new Rect(x, y, Math.min(w, width), height);
    }

    public Rect rightOf(int w, int gap) {
        var start = x + w + gap;
        return new Rect(start, y, Math.max(0, right() - start), height);
    }

    public Rect below(int h) {
        return new Rect(x, y + h, width, Math.max(0, height - h));
    }

    public Rect top(int h) {
        return new Rect(x, y, width, Math.min(h, height));
    }

    public boolean contains(double px, double py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }
}
