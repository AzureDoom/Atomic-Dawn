package mod.azure.atomicdawn.armor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ArmorPieceItem extends Item {

    private final ArmorSet set;

    private final ArmorPiece piece;

    public ArmorPieceItem(Properties properties, ArmorSet set, ArmorPiece piece) {
        super(properties);
        this.set = set;
        this.piece = piece;
    }

    public static Properties properties(Properties properties, ArmorSet set, ArmorPiece piece) {
        return properties.humanoidArmor(set.material(), piece.type());
    }

    public ArmorSet set() {
        return set;
    }

    public ArmorPiece piece() {
        return piece;
    }

    public static float condition(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 1F;
        }
        return 1F - stack.getDamageValue() / (float) stack.getMaxDamage();
    }
}
