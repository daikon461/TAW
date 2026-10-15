package dev.dualpower;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Registration-only prototype. Does not change Awakened's power. */
@Mod(AwakenedDualPowerMod.MOD_ID)
public final class AwakenedDualPowerMod {
    public static final String MOD_ID = "awakened_dual_power";
    private static final String SECOND_ID = "awakened_dual_power_second";

    public AwakenedDualPowerMod() { NeoForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dualpower")
            .then(Commands.literal("status").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                String second = player.getPersistentData().getString(SECOND_ID);
                String primary = AwakenedAttachments.get(player).powerId();
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "第1能力（読取専用）: " + primary + " / 第2能力（登録情報のみ）: " +
                    (second.isEmpty() ? "未登録" : second) + " / 切り替え機能は未実装"), false);
                return Command.SINGLE_SUCCESS;
            }))
            .then(Commands.literal("second").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("power_id", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "power_id");
                        if (!id.matches("[a-z0-9_.:-]{1,128}")) {
                            ctx.getSource().sendFailure(Component.literal("無効な能力ID形式です"));
                            return 0;
                        }
                        // Administrative metadata only: does NOT validate against Awakened's power registry.
                        player.getPersistentData().putString(SECOND_ID, id);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "第2能力IDを登録しました: " + id + "（実際の覚醒は未実装）"), false);
                        return Command.SINGLE_SUCCESS;
                    })))
                .then(Commands.literal("clear").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.getPersistentData().remove(SECOND_ID);
                    ctx.getSource().sendSuccess(() -> Component.literal("第2能力の登録情報を削除しました"), false);
                    return Command.SINGLE_SUCCESS;
                }))));
    }
}
