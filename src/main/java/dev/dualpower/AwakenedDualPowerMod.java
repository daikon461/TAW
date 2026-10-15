package dev.dualpower;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.SkillData;
import com.awakenedmod.awakened.core.skill.Stat;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.nbt.Tag;

/** Registration-only prototype. Does not change Awakened's power. */
@Mod(AwakenedDualPowerMod.MOD_ID)
public final class AwakenedDualPowerMod {
    public static final String MOD_ID = "awakened_dual_power";
    private static final String SECOND_ID = "awakened_dual_power_second";
    private static final String BACKUP_ID = "awakened_dual_power_checkpoint_v1";
    private static final String PRIMARY_ID = "awakened_dual_power_primary_id";
    private static final String SLOTS_ID = "awakened_dual_power_checkpoints_v2";

    /** Copies current Awakened data into our own namespace; never changes Awakened attachments. */
    private static void saveCheckpoint(ServerPlayer player) {
        SkillData skill = AwakenedAttachments.skill(player);
        CompoundTag checkpoint = new CompoundTag();
        checkpoint.putInt("schema", 1);
        checkpoint.putString("power_id", AwakenedAttachments.get(player).powerId());
        checkpoint.putInt("level", skill.level());
        checkpoint.putDouble("xp", skill.xp());
        CompoundTag points = new CompoundTag();
        for (Stat stat : Stat.values()) points.putInt(stat.name(), skill.points(stat));
        checkpoint.put("points", points);
        player.getPersistentData().put(BACKUP_ID, checkpoint);
    }

    /** Read-only snapshot for the currently active power. Does not modify Awakened. */
    private static boolean saveSlot(ServerPlayer player, int slot) {
        if (slot != 1 && slot != 2) return false;
        String current = AwakenedAttachments.get(player).powerId();
        if (current == null || current.isBlank()) return false;
        CompoundTag data = player.getPersistentData();
        String expected = slot == 1 ? data.getString(PRIMARY_ID) : data.getString(SECOND_ID);
        // Lock the first slot to its initial identity, preventing accidental overwrite
        // with a different Awakened power. Older snapshots are used as fallback.
        if (slot == 1 && expected.isBlank() && data.getCompound(SLOTS_ID).contains("slot1"))
            expected = data.getCompound(SLOTS_ID).getCompound("slot1").getString("power_id");
        if (slot == 1 && !expected.isBlank() && !expected.equals(current)) return false;
        // A slot 2 snapshot is only allowed when the actual Awakened power matches
        // its registered ID; never mislabel the first power as the second power.
        if (slot == 2 && (expected.isBlank() || !expected.equals(current))) return false;
        saveCheckpoint(player);
        if (slot == 1 && data.getString(PRIMARY_ID).isBlank()) data.putString(PRIMARY_ID, current);
        CompoundTag all = data.getCompound(SLOTS_ID).copy();
        CompoundTag checkpoint = data.getCompound(BACKUP_ID).copy();
        checkpoint.putInt("slot", slot);
        all.put("slot" + slot, checkpoint);
        data.put(SLOTS_ID, all);
        return true;
    }



    private static int saveSlotCommand(ServerPlayer player, int slot, net.minecraft.commands.CommandSourceStack source) {
        if (!saveSlot(player, slot)) {
            source.sendFailure(Component.literal("保存中止: 能力未覚醒、または第2能力IDが現在の能力と一致しません"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("第" + slot + "能力のスナップショットを保存しました (Awakened本体は変更しません)"), false);
        return Command.SINGLE_SUCCESS;
    }

    public AwakenedDualPowerMod() { NeoForge.EVENT_BUS.register(this); }

    /** Copies only addon-owned tags when Minecraft replaces a player after death.
     *  Does not touch Awakened's own attachments or gameplay effects.
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();
        for (String key : new String[]{SECOND_ID, PRIMARY_ID, BACKUP_ID, SLOTS_ID}) {
            if (oldData.contains(key)) {
                Tag value = oldData.get(key);
                if (value != null) newData.put(key, value.copy());
            }
        }
    }

    /** Read-only integrity report; never attempts to repair or switch powers. */
    private static String integrityReport(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag slots = data.getCompound(SLOTS_ID);
        String current = AwakenedAttachments.get(player).powerId();
        String primary = data.getString(PRIMARY_ID);
        String second = data.getString(SECOND_ID);
        boolean slot1 = slots.contains("slot1", Tag.TAG_COMPOUND);
        boolean slot2 = slots.contains("slot2", Tag.TAG_COMPOUND);
        String saved1 = slot1 ? slots.getCompound("slot1").getString("power_id") : "";
        String saved2 = slot2 ? slots.getCompound("slot2").getString("power_id") : "";
        java.util.List<String> issues = new java.util.ArrayList<>();
        if (current == null || current.isBlank()) issues.add("現在の能力IDが空です");
        if (slot1 && saved1.isBlank()) issues.add("第1スロットの能力IDが空です");
        if (slot2 && saved2.isBlank()) issues.add("第2スロットの能力IDが空です");
        if (!primary.isBlank() && slot1 && !primary.equals(saved1)) issues.add("第1能力IDが保存内容と不一致");
        if (!second.isBlank() && slot2 && !second.equals(saved2)) issues.add("第2能力IDが保存内容と不一致");
        if (!second.isBlank() && second.equals(primary)) issues.add("第1・第2能力IDが重複");
        if (slot2 && second.isBlank()) issues.add("第2スロットに記録があるのに登録IDがありません");
        for (int i = 1; i <= 2; i++) {
            if (!slots.contains("slot" + i, Tag.TAG_COMPOUND)) continue;
            CompoundTag slot = slots.getCompound("slot" + i);
            if (slot.getInt("schema") != 1) issues.add("スロット" + i + "の保存形式が不明です");
            if (slot.getInt("level") < 0 || !Double.isFinite(slot.getDouble("xp")) || slot.getDouble("xp") < 0)
                issues.add("スロット" + i + "の経験値データが不正です");
        }
        return "現在=" + current + " / 第1=" + (slot1 ? saved1 : "未保存")
            + " / 第2=" + (slot2 ? saved2 : "未保存") + " / 検査="
            + (issues.isEmpty() ? "異常なし（復元安全性は未検証）" : String.join("、", issues));
    }

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
            .then(Commands.literal("audit").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                String report = integrityReport(player);
                ctx.getSource().sendSuccess(() -> Component.literal(report), false);
                return Command.SINGLE_SUCCESS;
            }))
            .then(Commands.literal("slots")
                .then(Commands.literal("save1").executes(ctx -> saveSlotCommand(ctx.getSource().getPlayerOrException(), 1, ctx.getSource())))
                .then(Commands.literal("save2").executes(ctx -> saveSlotCommand(ctx.getSource().getPlayerOrException(), 2, ctx.getSource())))
                .then(Commands.literal("show").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    CompoundTag slots = player.getPersistentData().getCompound(SLOTS_ID);
                    String a = slots.contains("slot1") ? slots.getCompound("slot1").getString("power_id") : "未保存";
                    String b = slots.contains("slot2") ? slots.getCompound("slot2").getString("power_id") : "未保存";
                    ctx.getSource().sendSuccess(() -> Component.literal("保存済み能力: 第1=" + a + " / 第2=" + b + " (読み取り専用)"), false);
                    return Command.SINGLE_SUCCESS;
                })))
            .then(Commands.literal("checkpoint")
                .then(Commands.literal("save").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    saveCheckpoint(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "読み取り専用の能力・スキル記録を保存しました。Awakened本体は変更していません。"), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("show").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!player.getPersistentData().contains(BACKUP_ID)) {
                        ctx.getSource().sendFailure(Component.literal("記録がありません。/dualpower checkpoint save を実行してください"));
                        return 0;
                    }
                    CompoundTag saved = player.getPersistentData().getCompound(BACKUP_ID);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "保存済み: " + saved.getString("power_id") + " / Lv " + saved.getInt("level") +
                        " / XP " + saved.getDouble("xp") + " / points " + saved.getCompound("points")), false);
                    return Command.SINGLE_SUCCESS;
                })))
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
