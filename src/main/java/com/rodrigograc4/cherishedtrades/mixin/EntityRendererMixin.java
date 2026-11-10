package com.rodrigograc4.cherishedtrades.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rodrigograc4.cherishedtrades.VillagerLabelState;
import com.rodrigograc4.cherishedtrades.VillagerTradeLabels;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * Draws a villager's bookmarked trades above its head whenever vanilla would show its name, one line
 * per trade, the same way name tags are drawn but smaller and with no gaps. A custom name, if
 * any, goes right above them.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // Trade lines are drawn smaller than the name above them.
    @Unique
    private static final float TEXT_SCALE = 0.75F;
    // A name tag's background spans text rows -1..9, so a step of 10 makes lines touch.
    @Unique
    private static final int LINE_STEP = 10;
    // Name tags are drawn 0.5 above the attachment point, at 0.025 blocks per text pixel.
    @Unique
    private static final float PIXEL = 0.025F;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void cherishedTrades$startExtract(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        VillagerTradeLabels.startExtract();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cherishedTrades$extractTradeLines(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        List<Component> lines = VillagerTradeLabels.extract(entity, state.nameTag != null);
        ((VillagerLabelState) state).cherishedTrades$setTradeLines(lines);

        // Only set by vanilla when the entity has a name to show.
        if (!lines.isEmpty() && state.nameTagAttachment == null) {
            state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
        }
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At("HEAD"))
    private void cherishedTrades$submitTradeLines(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                                  CameraRenderState camera, int offset, CallbackInfo ci) {
        poseStack.pushPose();
        List<Component> lines = ((VillagerLabelState) state).cherishedTrades$getTradeLines();
        if (lines.isEmpty() || state.nameTagAttachment == null) return;

        // Scale around the point name tags are anchored to (attachment + 0.5 up), then hand
        // submitNameTag an attachment that cancels its own 0.5 so the anchor stays put.
        Vec3 attachment = state.nameTagAttachment;
        poseStack.pushPose();
        poseStack.translate(attachment.x, attachment.y + 0.5, attachment.z);
        poseStack.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
        Vec3 anchor = new Vec3(0.0, -0.5, 0.0);

        // The last line's bottom sits where a name's bottom would (row 9 at full size); the
        // others stack upwards from it, LINE_STEP rows apart. Rows grow downwards.
        int lastRow = Math.round(9 / TEXT_SCALE) - 9;
        int firstRow = lastRow - (lines.size() - 1) * LINE_STEP;
        for (int i = 0; i < lines.size(); i++) {
            collector.submitNameTag(poseStack, anchor, firstRow + i * LINE_STEP, lines.get(i), !state.isDiscrete, state.lightCoords, camera);
        }
        poseStack.popPose();

        // Raise the custom name, if any, so its bottom (row 9) meets the first line's top. It moves
        // along the camera's up direction, the one the lines are stacked along: plain world up
        // would leave a gap whenever the villager is seen from above or below.
        float firstTop = (firstRow - 1) * TEXT_SCALE;
        Vector3f up = new Vector3f(0.0F, (9 - firstTop) * PIXEL, 0.0F).rotate(camera.orientation);
        poseStack.translate(up.x, up.y, up.z);
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At("TAIL"))
    private void cherishedTrades$restorePose(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                             CameraRenderState camera, int offset, CallbackInfo ci) {
        poseStack.popPose();
    }
}
