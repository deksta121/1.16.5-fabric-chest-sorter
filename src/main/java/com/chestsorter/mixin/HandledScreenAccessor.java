package com.chestsorter.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int chestsorter$getX();

    @Accessor("y")
    int chestsorter$getY();

    @Accessor("backgroundWidth")
    int chestsorter$getBackgroundWidth();

    @Accessor("backgroundHeight")
    int chestsorter$getBackgroundHeight();
}
