package com.rins.spearswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Spear Swap (client-side, Fabric 1.21.11).
 *
 * Pegang spear + tahan/klik kiri -> mod swap ke slot swap, lalu di awal tick
 * berikutnya balik ke spear. Jadi lunge bisa di-spam.
 *
 * Tombol:
 *  - R : nyalakan / matikan mod
 *  - V : ganti slot swap (1-9), default slot 9
 */
public class SpearSwapClient implements ClientModInitializer {

    private static boolean enabled = true;
    private static int swapSlot = 8;      // index 0-8 (slot 9)
    private static int spearSlot = -1;
    private static boolean swapped = false;

    private static KeyBinding toggleKey;
    private static KeyBinding slotKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.spearswap.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyBinding.Category.MISC));
        slotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.spearswap.slot", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, KeyBinding.Category.MISC));

        // Awal tick: balik ke spear SEBELUM klik kiri diproses.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                swapped = false;
                return;
            }
            if (swapped) {
                if (spearSlot >= 0) {
                    client.player.getInventory().setSelectedSlot(spearSlot);
                }
                swapped = false;
            }
        });

        // Akhir tick: kalau klik kiri dipakai dengan spear, swap ke slot swap.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                msg(client, "Spear Swap: " + (enabled ? "ON" : "OFF"));
            }
            while (slotKey.wasPressed()) {
                swapSlot = (swapSlot + 1) % 9;
                msg(client, "Slot swap: " + (swapSlot + 1));
            }

            if (!enabled || client.player == null || client.currentScreen != null || swapped) {
                return;
            }

            int current = client.player.getInventory().getSelectedSlot();
            if (current == swapSlot) {
                return;
            }

            ItemStack held = client.player.getMainHandStack();
            if (client.options.attackKey.isPressed() && isSpear(held)) {
                spearSlot = current;
                client.player.getInventory().setSelectedSlot(swapSlot);
                swapped = true;
            }
        });
    }

    private static boolean isSpear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return Registries.ITEM.getId(stack.getItem()).getPath().endsWith("_spear");
    }

    private static void msg(MinecraftClient client, String text) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(text), true);
        }
    }
}
