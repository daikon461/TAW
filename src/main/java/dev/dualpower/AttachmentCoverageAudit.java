package dev.dualpower;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.SkillData;
import com.awakenedmod.awakened.core.data.ProgressData;
import com.awakenedmod.awakened.core.data.QuestData;
import com.awakenedmod.awakened.core.data.RelicData;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/** Read-only coverage probe for the five persistent Awakened attachment families. */
public final class AttachmentCoverageAudit {
    private AttachmentCoverageAudit() {}

    private static <T> boolean roundTrip(Codec<T> codec, T value) {
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value).result()
                .orElseThrow(() -> new IllegalStateException("encode failed"));
        T decoded = codec.parse(NbtOps.INSTANCE, encoded).result()
                .orElseThrow(() -> new IllegalStateException("decode failed"));
        Tag again = codec.encodeStart(NbtOps.INSTANCE, decoded).result()
                .orElseThrow(() -> new IllegalStateException("re-encode failed"));
        return encoded.equals(again);
    }

    public static String report(ServerPlayer player) {
        try {
            boolean power = roundTrip(PowerData.MAP_CODEC.codec(), AwakenedAttachments.get(player));
            boolean skill = roundTrip(SkillData.MAP_CODEC.codec(), AwakenedAttachments.skill(player));
            boolean progress = roundTrip(ProgressData.MAP_CODEC.codec(), AwakenedAttachments.progress(player));
            boolean quests = roundTrip(QuestData.MAP_CODEC.codec(), AwakenedAttachments.quests(player));
            boolean relics = roundTrip(RelicData.MAP_CODEC.codec(), AwakenedAttachments.relics(player));
            return "Awakened保存範囲: Power=" + label(power) + " Skill=" + label(skill)
                    + " Progress=" + label(progress) + " Quest=" + label(quests)
                    + " Relic=" + label(relics)
                    + " / 追加の一時状態・外見・飛行状態は未検証。能力変更なし";
        } catch (Exception ex) {
            return "保存範囲検査NG: " + ex.getClass().getSimpleName()
                    + " / 復元・切り替えは実行しません";
        }
    }

    private static String label(boolean ok) { return ok ? "OK" : "NG"; }
}
