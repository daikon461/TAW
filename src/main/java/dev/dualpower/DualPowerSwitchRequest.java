package dev.dualpower;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import com.awakenedmod.awakened.core.skill.Stat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server request. Power mutation is gated by administrator opt-in. */
public record DualPowerSwitchRequest() implements CustomPacketPayload {
    public static final Type<DualPowerSwitchRequest> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(AwakenedDualPowerMod.MOD_ID, "switch_request"));
    public static final StreamCodec<ByteBuf, DualPowerSwitchRequest> STREAM_CODEC =
        StreamCodec.unit(new DualPowerSwitchRequest());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // Per-session throttling. A successful experimental change starts cooldown.
    private static final String LAST_SUCCESS_MS = "awakened_dual_power_last_success_epoch_ms";
    private static final long COOLDOWN_MS = 5000L;

    private static void handle(DualPowerSwitchRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            // No trust in the client. The server decides if a switch is ready.
            var data = player.getPersistentData();
            String first = data.getString("awakened_dual_power_primary_id");
            String second = data.getString("awakened_dual_power_second");
            String current = AwakenedAttachments.get(player).powerId();
            int target = first.equals(current) ? 2 : second.equals(current) ? 1 : 0;
            if (target == 0 || first.isBlank() || second.isBlank() || first.equals(second)) {
                player.sendSystemMessage(Component.literal("[Dual Power] 現在の能力と登録情報が一致しません。切り替え中止。"));
                return;
            }
            CompoundTag slots = data.getCompound("awakened_dual_power_checkpoints_v2");
            String expected = target == 1 ? first : second;
            String key = "slot" + target;
            if (!slots.contains(key, Tag.TAG_COMPOUND)) {
                player.sendSystemMessage(Component.literal("[Dual Power] スロット" + target + "の保存情報がありません。"));
                return;
            }
            CompoundTag snapshot = slots.getCompound(key);
            if (snapshot.getInt("schema") != 1 || !expected.equals(snapshot.getString("power_id"))
                    || snapshot.getInt("level") < 0 || !Double.isFinite(snapshot.getDouble("xp"))
                    || snapshot.getDouble("xp") < 0 || !snapshot.contains("points", Tag.TAG_COMPOUND)) {
                player.sendSystemMessage(Component.literal("[Dual Power] 保存データが不正です。切り替え中止。"));
                return;
            }
            CompoundTag points = snapshot.getCompound("points");
            for (Stat stat : Stat.values()) {
                if (!points.contains(stat.name(), Tag.TAG_INT) || points.getInt(stat.name()) < 0) {
                    player.sendSystemMessage(Component.literal("[Dual Power] スキル情報が不正です。切り替え中止。"));
                    return;
                }
            }
            // Persist wall-clock time, not level gameTime (which may reset on restart).
            long now = System.currentTimeMillis();
            long previous = data.getLong(LAST_SUCCESS_MS);
            if (data.contains(LAST_SUCCESS_MS, Tag.TAG_LONG)) {
                long elapsed = now - previous;
                // A clock rollback must not bypass the cooldown. The next successful
                // switch will update the timestamp after the clock is corrected.
                if (elapsed < 0L || elapsed < COOLDOWN_MS) {
                    long remaining = elapsed < 0L ? COOLDOWN_MS : COOLDOWN_MS - elapsed;
                    player.sendSystemMessage(Component.literal(
                        "[Dual Power] 切り替え待機: あと約" + ((remaining + 999L) / 1000L) + "秒"));
                    return;
                }
            }
            // Opt-in only: normal players remain on the read-only preview path.
            if (!data.getBoolean(ExperimentalPowerSwitch.ENABLE_FLAG)) {
                player.sendSystemMessage(Component.literal(
                    "[Dual Power] スロット" + target + "の検査成功。実際の能力変更は無効です。"));
                return;
            }
            if (data.getBoolean("awakened_dual_power_transaction_pending")
                    || data.contains("awakened_dual_power_recovery_snapshot", Tag.TAG_COMPOUND)) {
                player.sendSystemMessage(Component.literal(
                    "[Dual Power] 未完了の切り替えが記録されています。安全のため中止しました。"));
                return;
            }
            // Defense in depth: the network path must never mutate Awakened in this release.
            if (!FullStateSafetyGate.canSwitch()) {
                player.sendSystemMessage(Component.literal(
                    "[Dual Power] 安全ロック中: " + FullAwakenedSnapshot.capture(player)));
                return;
            }
            String before = current;
            String result = ExperimentalPowerSwitch.execute(player);
            String after = AwakenedAttachments.get(player).powerId();
            // A successful verified power change is the only event that starts cooldown.
            if (!before.equals(after) && expected.equals(after)
                    && !data.getBoolean("awakened_dual_power_transaction_pending")) {
                data.putLong(LAST_SUCCESS_MS, now);
            }
            player.sendSystemMessage(Component.literal("[Dual Power] " + result));
        });
    }

    @EventBusSubscriber(modid = AwakenedDualPowerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, DualPowerSwitchRequest::handle);
        }
    }
}
