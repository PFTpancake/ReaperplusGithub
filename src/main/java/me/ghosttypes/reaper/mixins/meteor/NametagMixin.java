package me.ghosttypes.reaper.mixins.meteor;

import me.ghosttypes.reaper.modules.render.Nametags;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// have to do this so the nametag replacement doesn't render the vanilla nametag behind the meteor ones
@Mixin(EntityRenderer.class)
public abstract class NametagMixin<T extends Entity> implements IEntityRenderer {
    @Shadow
    public abstract Identifier getTexture(Entity entity);
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        if (!(entity instanceof PlayerEntity)) return;
        if (Modules.get().isActive(Nametags.class)) info.cancel();
    }
}
