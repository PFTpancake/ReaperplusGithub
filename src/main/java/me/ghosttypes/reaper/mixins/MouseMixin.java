package me.ghosttypes.reaper.mixins;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("RETURN"))
    private void onMouse(long window, int button, int action, int mods, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(KeyEvent.get(button, 0, KeyAction.get(action)));
    }
}
