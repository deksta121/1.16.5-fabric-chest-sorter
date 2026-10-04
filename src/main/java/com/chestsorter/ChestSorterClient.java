package com.chestsorter;

import com.chestsorter.mixin.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ChestSorterClient implements ClientModInitializer {
    private static final int BTN = 12;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof HandledScreen)) return;

            HandledScreen<?> hs = (HandledScreen<?>) screen;
            ScreenHandler handler = hs.getScreenHandler();
            HandledScreenAccessor acc = (HandledScreenAccessor) hs;

            if (isSortableContainer(handler)) {
                // Кнопка "S" справа вверху сундука - сортирует сундук.
                addButton(hs, acc, "Отсортировать сундук",
                        () -> containerSlots(handler),
                        () -> acc.chestsorter$getX() + acc.chestsorter$getBackgroundWidth() - BTN - 6,
                        () -> acc.chestsorter$getY() + 4);
            }
        });
    }

    private static boolean isSortableContainer(ScreenHandler handler) {
        return handler instanceof GenericContainerScreenHandler
                || handler instanceof ShulkerBoxScreenHandler
                || handler instanceof HopperScreenHandler
                || handler instanceof Generic3x3ContainerScreenHandler;
    }

    /** Слоты самого контейнера (всё, что не относится к инвентарю игрока). */
    private static List<Slot> containerSlots(ScreenHandler handler) {
        List<Slot> result = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof PlayerInventory)) {
                result.add(slot);
            }
        }
        return result;
    }

    private static void addButton(HandledScreen<?> screen, HandledScreenAccessor acc, String tooltip,
                                  Supplier<List<Slot>> slots, Supplier<Integer> xPos, Supplier<Integer> yPos) {
        ButtonWidget button = new ButtonWidget(xPos.get(), yPos.get(), BTN, BTN, new LiteralText("S"),
                b -> Sorter.sort(slots.get()));
        Screens.getButtons(screen).add(button);

        // Позиция обновляется каждый кадр (например, при открытии книги рецептов).
        ScreenEvents.beforeRender(screen).register((s, matrices, mouseX, mouseY, tickDelta) -> {
            button.x = xPos.get();
            button.y = yPos.get();
        });
        ScreenEvents.afterRender(screen).register((s, matrices, mouseX, mouseY, tickDelta) -> {
            if (button.isHovered()) {
                s.renderTooltip(matrices, new LiteralText(tooltip), mouseX, mouseY);
            }
        });
    }
}
