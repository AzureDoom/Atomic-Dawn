package mod.azure.atomicdawn.client.helper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public final class ADGuiInput {

    private ADGuiInput() {}

    public static Mouse mouse(MouseButtonEvent event) {
        return new Mouse(event);
    }

    public static Key key(KeyEvent event) {
        return new Key(event);
    }

    public static boolean matches(KeyMapping mapping, Key key) {
        return mapping.matches(key.raw);
    }

    public static final class Mouse {

        final MouseButtonEvent raw;

        private Mouse(MouseButtonEvent raw) {
            this.raw = raw;
        }

        public double x() {
            return raw.x();
        }

        public double y() {
            return raw.y();
        }

        public int button() {
            return raw.button();
        }

        public boolean hasShiftDown() {
            return raw.hasShiftDown();
        }
    }

    public static final class Key {

        final KeyEvent raw;

        private Key(KeyEvent raw) {
            this.raw = raw;
        }

        public int key() {
            return raw.key();
        }

        public int modifiers() {
            return raw.modifiers();
        }
    }
}
