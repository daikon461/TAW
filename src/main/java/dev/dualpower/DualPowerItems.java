package dev.dualpower;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The recipe is live; actual second-awakening remains fail-closed until full rollback is proven. */
public final class DualPowerItems {
    private DualPowerItems() {}
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AwakenedDualPowerMod.MOD_ID);
    public static final DeferredItem<Item> SECOND_AWAKENING_CRYSTAL = ITEMS.registerItem(
        "second_awakening_crystal", properties -> new Item(properties.stacksTo(1)) {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.awakened_dual_power.crystal_locked"), false);
                }
                // Never consume or mutate any Awakened state while the safety lock is active.
                return InteractionResultHolder.pass(stack);
            }
        });
}
