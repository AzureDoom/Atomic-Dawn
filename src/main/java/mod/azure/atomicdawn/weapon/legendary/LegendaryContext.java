package mod.azure.atomicdawn.weapon.legendary;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.weapon.action.ShooterState;

public record LegendaryContext(
    ServerLevel level,
    ServerPlayer attacker,
    ItemStack weapon,
    Entity target,
    Vec3 hitPosition,
    ShooterState shooter
) {}
