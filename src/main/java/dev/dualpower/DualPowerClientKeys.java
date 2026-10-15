package dev.dualpower;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Alt+V client-side input preview only. No packets or Awakened data mutations.
 * Separate client-only event subscribers avoid loading Minecraft client classes
 * on a dedicated server.
 */
public final class DualPowerClientKeys {
    private DualPowerClientKeys() {}

    private static final KeyMapping PREVIEW = new KeyMapping(
        "key.awakened_dual_power.preview", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V, "key.categories.awakened_dual_power");

    @EventBusSubscriber(modid = AwakenedDualPowerMod.MOD_ID,
                        bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent event) {
            event.register(PREVIEW);
        }
    }

    @EventBusSubscriber(modid = AwakenedDualPowerMod.MOD_ID, value = Dist.CLIENT)
    public static final class TickHandler {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            while (PREVIEW.consumeClick()) {
                if (minecraft.player == null || minecraft.screen != null) continue;
                long window = minecraft.getWindow().getWindow();
                boolean alt = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                           || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
                if (alt) {
                    PacketDistributor.sendToServer(new DualPowerSwitchRequest());
                    minecraft.player.displayClientMessage(Component.translatable(
                        "message.awakened_dual_power.preview"), true);
                }
            }
        }
    }
}
