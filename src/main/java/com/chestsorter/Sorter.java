package com.chestsorter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.registry.Registry;

import java.util.Comparator;
import java.util.List;

/**
 * Сортировка через обычные клики по слотам (как будто игрок делает это руками),
 * поэтому работает и в одиночной игре, и на серверах.
 */
public final class Sorter {
    private Sorter() {
    }

    /** Порядок: по вкладке креатива, затем по id предмета, затем по NBT. Пустые слоты - в конец. */
    private static final Comparator<ItemStack> ORDER = (a, b) -> {
        if (a.isEmpty() && b.isEmpty()) return 0;
        if (a.isEmpty()) return 1;
        if (b.isEmpty()) return -1;

        int c = Integer.compare(groupIndex(a), groupIndex(b));
        if (c != 0) return c;

        c = Registry.ITEM.getId(a.getItem()).toString().compareTo(Registry.ITEM.getId(b.getItem()).toString());
        if (c != 0) return c;

        String ta = a.getTag() == null ? "" : a.getTag().toString();
        String tb = b.getTag() == null ? "" : b.getTag().toString();
        return ta.compareTo(tb);
    };

    private static int groupIndex(ItemStack stack) {
        ItemGroup group = stack.getItem().getGroup();
        return group == null ? Integer.MAX_VALUE : group.getIndex();
    }

    /** Объединяет одинаковые предметы в стаки и сортирует переданные слоты. */
    public static void sort(List<Slot> slots) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager manager = client.interactionManager;
        if (player == null || manager == null || slots.isEmpty()) return;

        // Если в руке (на курсоре) что-то лежит - клики сломают порядок.
        if (!player.inventory.getCursorStack().isEmpty()) {
            player.sendMessage(new net.minecraft.text.LiteralText("Chest Sorter: уберите предмет с курсора"), true);
            return;
        }

        ScreenHandler handler = player.currentScreenHandler;
        Ctx ctx = new Ctx(client, player, manager, handler);

        merge(ctx, slots);
        sortSlots(ctx, slots);
    }

    private static void merge(Ctx ctx, List<Slot> slots) {
        int n = slots.size();
        for (int i = 0; i < n; i++) {
            ItemStack target = slots.get(i).getStack();
            if (target.isEmpty() || target.getCount() >= target.getMaxCount()) continue;

            for (int j = i + 1; j < n; j++) {
                ItemStack source = slots.get(j).getStack();
                if (source.isEmpty() || !ItemStack.canCombine(target, source)) continue;

                click(ctx, slots.get(j));              // берём стак из j
                click(ctx, slots.get(i));              // докладываем в i
                if (!ctx.player.inventory.getCursorStack().isEmpty()) {
                    click(ctx, slots.get(j));          // остаток возвращаем
                }

                target = slots.get(i).getStack();
                if (target.getCount() >= target.getMaxCount()) break;
            }
        }
    }

    private static void sortSlots(Ctx ctx, List<Slot> slots) {
        int n = slots.size();
        for (int k = 0; k < n; k++) {
            int best = k;
            for (int m = k + 1; m < n; m++) {
                if (ORDER.compare(slots.get(m).getStack(), slots.get(best).getStack()) < 0) {
                    best = m;
                }
            }
            if (best != k) {
                swap(ctx, slots.get(k), slots.get(best));
            }
        }
    }

    private static void swap(Ctx ctx, Slot a, Slot b) {
        ItemStack sa = a.getStack();
        ItemStack sb = b.getStack();
        // Одинаковые стаки при клике слились бы, а не поменялись местами.
        if (!sa.isEmpty() && !sb.isEmpty() && ItemStack.canCombine(sa, sb)) return;

        if (sa.isEmpty()) {
            click(ctx, b);
            click(ctx, a);
        } else {
            click(ctx, a);
            click(ctx, b);
            if (!ctx.player.inventory.getCursorStack().isEmpty()) {
                click(ctx, a);
            }
        }
    }

    private static void click(Ctx ctx, Slot slot) {
        ctx.manager.clickSlot(ctx.handler.syncId, slot.id, 0, SlotActionType.PICKUP, ctx.player);
    }

    private static final class Ctx {
        final MinecraftClient client;
        final ClientPlayerEntity player;
        final ClientPlayerInteractionManager manager;
        final ScreenHandler handler;

        Ctx(MinecraftClient client, ClientPlayerEntity player, ClientPlayerInteractionManager manager, ScreenHandler handler) {
            this.client = client;
            this.player = player;
            this.manager = manager;
            this.handler = handler;
        }
    }
}
