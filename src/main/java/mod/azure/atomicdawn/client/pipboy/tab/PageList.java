package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;

final class PageList {

    static final int ROW = 13;

    static final int TOOLBAR = 15;

    private PageList() {}

    static int listWidth(Rect area) {
        return Math.max(120, area.width() * 2 / 5);
    }

    static Rect list(Rect area, boolean toolbar) {
        var body = toolbar ? area.below(TOOLBAR) : area;
        return body.left(listWidth(area));
    }

    static Rect detail(Rect area) {
        return area.rightOf(listWidth(area), 10);
    }

    static int rows(Rect list) {
        return Math.max(1, list.height() / ROW);
    }

    static int first(String key, int count, int rows) {
        var max = Math.max(0, count - rows);
        var value = Math.clamp(PipBoyClientState.scroll(key), 0, max);
        PipBoyClientState.setScroll(key, value);
        return value;
    }

    static boolean scroll(String key, int direction, int count, int rows) {
        var max = Math.max(0, count - rows);
        var next = Math.clamp(PipBoyClientState.scroll(key) + direction, 0, max);
        if (next == PipBoyClientState.scroll(key)) {
            return false;
        }
        PipBoyClientState.setScroll(key, next);
        return true;
    }

    static void decorate(GuiGraphicsExtractor graphics, Rect list, Rect detail, String key, int count) {
        graphics.fill(detail.x() - 5, detail.y(), detail.x() - 4, detail.bottom(), PipBoyTheme.faint());
        var rows = rows(list);
        if (count <= rows) {
            return;
        }
        var first = PipBoyClientState.scroll(key);
        var thumbH = Math.max(8, list.height() * rows / count);
        var thumbY = list.y() + (list.height() - thumbH) * first / Math.max(1, count - rows);
        graphics.fill(list.right() + 1, list.y(), list.right() + 3, list.bottom(), PipBoyTheme.faint());
        graphics.fill(list.right() + 1, thumbY, list.right() + 3, thumbY + thumbH, PipBoyTheme.text());
    }
}
