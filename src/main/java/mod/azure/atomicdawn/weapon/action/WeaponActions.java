package mod.azure.atomicdawn.weapon.action;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ammunition.AmmoInventory;
import mod.azure.atomicdawn.armor.upgrade.ArmorEffectType;
import mod.azure.atomicdawn.armor.upgrade.ArmorEvents;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.ReloadStatusPayload;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.pipboy.PipBoyService;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorEffectType;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgradeEvents;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.weapon.action.ShooterState.ReloadPhase;
import mod.azure.atomicdawn.weapon.combat.HitscanResolver;
import mod.azure.atomicdawn.weapon.combat.MeleeResolver;
import mod.azure.atomicdawn.weapon.combat.ProjectileLauncher;
import mod.azure.atomicdawn.weapon.combat.SpreadModel;
import mod.azure.atomicdawn.weapon.definition.FireMode;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class WeaponActions {

    private WeaponActions() {}

    public static ShooterState shooter(Player player) {
        return player.getData(ADAttachments.SHOOTER_STATE.get());
    }

    public static boolean isAiming(Player player, ItemStack stack) {
        return player.isUsingItem()
            && player.getUsedItemHand() == InteractionHand.MAIN_HAND
            && Weapons.isWeapon(stack)
            && player.getUseItem().is(stack.getItem());
    }

    public static void tryFire(ServerPlayer player) {
        var stack = player.getMainHandItem();
        var definition = usableDefinition(player, stack);
        if (definition == null) {
            return;
        }
        var shooter = shooter(player);
        var mode = Weapons.state(stack, definition).fireMode();
        var now = player.level().getGameTime();
        switch (mode) {
            case CHARGED -> {

            }
            case SPIN_UP -> {
                if (shooter.triggerHeld && now - shooter.triggerPressTick >= definition.firing().spinUpTicks()) {
                    fireOnce(player, stack, definition, 1F);
                }
            }
            case BURST -> {
                if (shooter.burstRemaining <= 0 && fireOnce(player, stack, definition, 1F)) {
                    shooter.burstRemaining = definition.firing().burst().count() - 1;
                }
            }
            default -> fireOnce(player, stack, definition, 1F);
        }
    }

    public static void triggerPress(ServerPlayer player) {
        var stack = player.getMainHandItem();
        var definition = usableDefinition(player, stack);
        if (definition == null) {
            return;
        }
        var shooter = shooter(player);
        var mode = Weapons.state(stack, definition).fireMode();
        if (!mode.tracksTrigger() || shooter.triggerHeld || shooter.isReloading()) {
            return;
        }
        shooter.triggerHeld = true;
        shooter.triggerPressTick = player.level().getGameTime();
        if (mode == FireMode.CHARGED) {
            WeaponAnimations.charge(player, stack);
        } else {
            WeaponAnimations.spinUp(player, stack);
        }
    }

    public static void triggerRelease(ServerPlayer player) {
        var shooter = shooter(player);
        if (!shooter.triggerHeld) {
            return;
        }
        shooter.triggerHeld = false;
        var stack = player.getMainHandItem();
        var definition = usableDefinition(player, stack);
        if (definition == null) {
            return;
        }
        var mode = Weapons.state(stack, definition).fireMode();
        if (mode == FireMode.CHARGED) {
            var held = player.level().getGameTime() - shooter.triggerPressTick;
            var fraction = Math.min(1F, held / (float) definition.firing().charge().ticks());
            var multiplier = definition.firing().charge().damageFor(fraction);
            if (multiplier > 0F) {
                fireOnce(player, stack, definition, multiplier);
            } else {
                WeaponAnimations.aim(player, stack, shooter.aiming);
            }
        } else if (mode == FireMode.SPIN_UP) {
            WeaponAnimations.spinDown(player, stack);
        }
    }

    private static boolean fireOnce(
        ServerPlayer player,
        ItemStack stack,
        WeaponDefinition definition,
        float multiplier
    ) {
        var level = player.level();
        var shooter = shooter(player);
        var now = level.getGameTime();
        shooter.lastChargeMultiplier = multiplier;
        if (
            now < shooter.drawReadyTick || now + AtomicDawn.CONFIG.weapons.FIRE_TIMING_TOLERANCE < shooter.nextShotTick
        ) {
            return false;
        }

        var state = Weapons.ensureState(stack, definition);

        if (shooter.isReloading()) {
            var interruptible = definition.reload().type().perRound()
                && state.loadedAmmo() >= definition.ammo().perShot();
            if (!interruptible) {
                return false;
            }
            shooter.clearReload();
            ADNetwork.sendTo(player, ReloadStatusPayload.IDLE);
        }

        var condition = Weapons.condition(stack, definition);
        if (condition.isBroken()) {
            if (now >= shooter.nextBrokenMessageTick) {
                shooter.nextBrokenMessageTick = now + 40;
                player.sendOverlayMessage(Component.translatable("message.atomicdawn.weapon_broken"));
                playSound(player, definition.sounds().dryFire().orElse(ADSounds.DRY_FIRE), 0.6F);
            }
            return false;
        }

        var vatsShot = VatsActions.prepare(player, shooter, definition);
        if (vatsShot != null && !VatsActions.canAfford(player, definition)) {
            shooter.burstRemaining = 0;
            if (now >= shooter.nextDryFireTick) {
                shooter.nextDryFireTick = now + 6 * 3;
                VatsActions.notEnoughAp(player);
            }
            return false;
        }

        if (definition.usesAmmo()) {
            var perShot = definition.ammo().perShot();
            if (state.loadedAmmo() < perShot) {
                if (now >= shooter.nextDryFireTick) {
                    shooter.nextDryFireTick = now + 6;
                    playSound(player, definition.sounds().dryFire().orElse(ADSounds.DRY_FIRE), 0.8F);
                }
                shooter.burstRemaining = 0;
                if (AtomicDawn.CONFIG.weapons.AUTO_RELOAD_ON_EMPTY) {
                    tryReload(player);
                }
                return false;
            }
            var automaticFire = state.fireMode().repeatsWhileHeld();
            if (!PerkHooks.saveAmmo(player, definition.category(), automaticFire)) {
                Weapons.setState(stack, state.withLoadedAmmo(state.loadedAmmo() - perShot));
            }
        }

        shooter.nextShotTick = Math.max(shooter.nextShotTick, now) + definition.firing().intervalTicks();
        shooter.lastShotTick = now;

        var aiming = isAiming(player, stack);
        var damageMultiplier = multiplier * condition.damageMultiplier();
        boolean used;
        if (definition.isProjectile()) {
            var accuracy = definition.accuracy();
            var cone = SpreadModel.coneDegrees(player, definition, shooter.bloom, aiming);
            var aimAt = vatsShot != null ? VatsActions.spendForProjectile(player, definition, shooter, vatsShot) : null;
            ProjectileLauncher.launch(level, player, stack, definition, damageMultiplier, cone, aimAt);
            shooter.bloom = Math.min(accuracy.maxBloom(), shooter.bloom + accuracy.bloomPerShot());
            used = true;
        } else if (vatsShot != null) {
            VatsActions.resolve(level, player, stack, definition, shooter, vatsShot, damageMultiplier);
            if (!definition.isMelee()) {
                var accuracy = definition.accuracy();
                shooter.bloom = Math.min(accuracy.maxBloom(), shooter.bloom + accuracy.bloomPerShot());
            }
            used = true;
        } else if (definition.isMelee()) {
            used = MeleeResolver.swing(level, player, stack, definition, shooter, damageMultiplier);
        } else {
            var accuracy = definition.accuracy();
            var cone = SpreadModel.coneDegrees(player, definition, shooter.bloom, aiming);
            HitscanResolver.fire(level, player, stack, definition, shooter, cone, damageMultiplier);
            shooter.bloom = Math.min(accuracy.maxBloom(), shooter.bloom + accuracy.bloomPerShot());
            used = true;
        }
        if (used) {
            Weapons.wear(
                stack,
                definition,
                player.getRandom(),
                SpecialStatResolver.resolve(player).conditionLossMultiplier()
                    * PowerArmorUpgradeEvents.reductionMultiplier(player, PowerArmorEffectType.CONDITION_LOSS)
                    * ArmorEvents.reductionMultiplier(player, ArmorEffectType.CONDITION_LOSS)
            );
            PipBoyService.recordShot(player);
        }

        playSound(player, definition.sounds().fire(), 1.0F + (player.getRandom().nextFloat() - 0.5F) * 0.08F);
        WeaponAnimations.fire(player, stack, aiming);
        return true;
    }

    public static void tryReload(ServerPlayer player) {
        var stack = player.getMainHandItem();
        var level = player.level();
        var definition = Weapons.definition(level, stack);
        if (definition == null || !definition.usesAmmo() || !player.isAlive()) {
            return;
        }
        var shooter = shooter(player);
        var now = level.getGameTime();
        if (shooter.isReloading() || shooter.triggerHeld || now < shooter.drawReadyTick) {
            return;
        }

        var state = Weapons.ensureState(stack, definition);
        var ammo = definition.ammo();
        if (state.loadedAmmo() >= ammo.magazineSize()) {
            return;
        }
        var reserve = AmmoInventory.hasInfiniteAmmo(player)
            ? Integer.MAX_VALUE
            : AmmoInventory.count(player, ammo.item());
        if (reserve <= 0) {
            return;
        }

        var reload = definition.reload();
        var empty = state.isEmpty();
        var slot = player.getInventory().getSelectedSlot();
        var speed = reloadSpeed(player, definition, stack);
        shooter.burstRemaining = 0;
        if (reload.type().perRound()) {
            shooter.startReload(
                ReloadPhase.START,
                now + PerkHooks.scaleTicks(reload.ticks(), speed),
                slot,
                stack.getItem()
            );
            WeaponAnimations.reloadStart(player, stack);
        } else {
            shooter.startReload(
                ReloadPhase.MAGAZINE,
                now + PerkHooks.scaleTicks(reload.ticksFor(empty), speed),
                slot,
                stack.getItem()
            );
            WeaponAnimations.reload(player, stack, empty);
        }
        definition.sounds().reload().ifPresent(sound -> playSound(player, sound, 1F));

        var needed = Math.min(reserve, ammo.magazineSize() - state.loadedAmmo());
        ADNetwork.sendTo(
            player,
            new ReloadStatusPayload(PerkHooks.scaleTicks(reload.estimateTicks(needed, empty), speed))
        );
    }

    private static float reloadSpeed(ServerPlayer player, WeaponDefinition definition, ItemStack stack) {
        var automatic = Weapons.state(stack, definition).fireMode().repeatsWhileHeld();
        return PerkHooks.reloadMultiplier(player, definition.category(), automatic);
    }

    private static void tickReload(ServerPlayer player, ShooterState shooter, long now) {
        var stack = player.getMainHandItem();
        var stillHolding = player.isAlive()
            && player.getInventory().getSelectedSlot() == shooter.reloadSlot
            && stack.getItem() == shooter.reloadItem;

        if (!stillHolding) {
            shooter.clearReload();
            ADNetwork.sendTo(player, ReloadStatusPayload.IDLE);
            return;
        }
        if (now < shooter.reloadNextTick) {
            return;
        }
        var definition = Weapons.definition(player.level(), stack);
        if (definition == null) {
            finishReload(player, shooter, stack, now);
            return;
        }
        var reload = definition.reload();
        var magazine = definition.ammo().magazineSize();

        switch (shooter.reloadPhase) {
            case MAGAZINE -> {
                var state = Weapons.ensureState(stack, definition);
                var loaded = AmmoInventory.take(player, definition.ammo().item(), magazine - state.loadedAmmo());
                Weapons.setState(stack, state.withLoadedAmmo(state.loadedAmmo() + loaded));
                finishReload(player, shooter, stack, now);
            }
            case START -> {
                shooter.reloadPhase = ReloadPhase.INSERTING;
                shooter.reloadNextTick = now;
                tickReload(player, shooter, now);
            }
            case INSERTING -> {
                var state = Weapons.ensureState(stack, definition);
                var inserted = state.loadedAmmo() < magazine
                    ? AmmoInventory.take(player, definition.ammo().item(), 1)
                    : 0;
                if (inserted > 0) {
                    state = state.withLoadedAmmo(state.loadedAmmo() + inserted);
                    Weapons.setState(stack, state);
                    WeaponAnimations.reloadInsert(player, stack);
                    definition.sounds().reloadInsert().ifPresent(sound -> playSound(player, sound, 1F));
                }
                var full = state.loadedAmmo() >= magazine;
                var outOfAmmo = !AmmoInventory.hasInfiniteAmmo(player)
                    && AmmoInventory.count(player, definition.ammo().item()) <= 0;
                if (inserted == 0 || full || outOfAmmo) {
                    shooter.reloadPhase = ReloadPhase.END;
                }
                shooter.reloadNextTick = now + (inserted > 0
                    ? PerkHooks.scaleTicks(reload.perRoundTicks(), reloadSpeed(player, definition, stack))
                    : 0);
            }
            case END -> {
                WeaponAnimations.reloadEnd(player, stack);
                shooter.reloadPhase = ReloadPhase.FINISH;
                shooter.reloadNextTick = now
                    + PerkHooks.scaleTicks(reload.endTicks(), reloadSpeed(player, definition, stack));
            }
            case FINISH -> finishReload(player, shooter, stack, now);
            case IDLE -> {}
        }
    }

    private static void finishReload(ServerPlayer player, ShooterState shooter, ItemStack stack, long now) {
        shooter.clearReload();
        shooter.nextShotTick = Math.min(shooter.nextShotTick, now);
        if (shooter.aiming) {
            WeaponAnimations.aim(player, stack, true);
        }
        ADNetwork.sendTo(player, ReloadStatusPayload.IDLE);
    }

    public static void cycleFireMode(ServerPlayer player) {
        var stack = player.getMainHandItem();
        var definition = Weapons.definition(player.level(), stack);
        if (definition == null || definition.firing().modes().size() < 2) {
            return;
        }
        var shooter = shooter(player);
        shooter.triggerHeld = false;
        shooter.burstRemaining = 0;
        var state = Weapons.ensureState(stack, definition);
        Weapons.setState(stack, state.withFireMode(definition.firing().next(state.fireMode())));
        playSound(player, ADSounds.DRY_FIRE, 0.4F);
    }

    public static void inspect(ServerPlayer player) {
        var stack = player.getMainHandItem();
        var definition = Weapons.definition(player.level(), stack);
        if (definition == null) {
            return;
        }
        var shooter = shooter(player);
        if (shooter.isReloading() || shooter.aiming || player.level().getGameTime() < shooter.drawReadyTick) {
            return;
        }
        WeaponAnimations.inspect(player, stack);
    }

    public static void setAiming(ServerPlayer player, ItemStack stack, boolean aiming) {
        var shooter = shooter(player);
        if (shooter.aiming == aiming) {
            return;
        }
        shooter.aiming = aiming;
        if (!shooter.isReloading() && !shooter.triggerHeld) {
            WeaponAnimations.aim(player, stack, aiming);
        }
    }

    public static void tick(ServerPlayer player) {
        var shooter = shooter(player);
        var level = player.level();
        var now = level.getGameTime();

        trackDraw(player, shooter, now);
        VatsActions.tick(player, shooter, now);

        if (shooter.isReloading()) {
            tickReload(player, shooter, now);
        }

        if (shooter.burstRemaining > 0) {
            tickBurst(player, shooter, now);
        }

        if (shooter.bloom > 0F && now - shooter.lastShotTick > 2) {
            var definition = Weapons.definition(level, player.getMainHandItem());
            var recovery = definition != null
                ? definition.accuracy().bloomRecoveryPerTick()
                : WeaponDefinition.Accuracy.DEFAULT.bloomRecoveryPerTick();
            shooter.bloom = Math.max(0F, shooter.bloom - recovery);
        }

        if (shooter.aiming && !isAiming(player, player.getMainHandItem())) {
            shooter.aiming = false;
        }
    }

    private static void tickBurst(ServerPlayer player, ShooterState shooter, long now) {
        var stack = player.getMainHandItem();
        var definition = usableDefinition(player, stack);
        if (definition == null || Weapons.state(stack, definition).fireMode() != FireMode.BURST) {
            shooter.burstRemaining = 0;
            return;
        }
        if (now + AtomicDawn.CONFIG.weapons.FIRE_TIMING_TOLERANCE < shooter.nextShotTick) {
            return;
        }
        if (!fireOnce(player, stack, definition, 1F)) {
            shooter.burstRemaining = 0;
            return;
        }
        shooter.burstRemaining--;
        if (shooter.burstRemaining == 0) {
            shooter.nextShotTick += definition.firing().burst().cooldownTicks();
        }
    }

    private static void trackDraw(ServerPlayer player, ShooterState shooter, long now) {
        var slot = player.getInventory().getSelectedSlot();
        var stack = player.getMainHandItem();
        var item = stack.getItem();
        if (slot == shooter.lastSelectedSlot && item == shooter.lastHeldItem) {
            return;
        }
        shooter.lastSelectedSlot = slot;
        shooter.lastHeldItem = item;
        shooter.aiming = false;
        shooter.triggerHeld = false;
        shooter.burstRemaining = 0;
        VatsActions.reset(shooter);

        var definition = Weapons.definition(player.level(), stack);
        if (definition == null) {
            return;
        }
        Weapons.ensureState(stack, definition);
        shooter.drawReadyTick = now + definition.drawTicks();
        shooter.nextShotTick = Math.max(shooter.nextShotTick, shooter.drawReadyTick);
        definition.sounds().draw().ifPresent(sound -> playSound(player, sound, 0.7F));
        WeaponAnimations.draw(player, stack);
    }

    private static WeaponDefinition usableDefinition(ServerPlayer player, ItemStack stack) {
        if (!player.isAlive() || player.isSpectator()) {
            return null;
        }
        return Weapons.definition(player.level(), stack);
    }

    private static void playSound(ServerPlayer player, Holder<SoundEvent> sound, float pitch) {
        ServerLevel level = player.level();
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            sound.value(),
            SoundSource.PLAYERS,
            1F,
            pitch
        );
    }
}
