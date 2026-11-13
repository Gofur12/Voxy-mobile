package dev.brezze.voxymobile.fabric.mixin;

import dev.brezze.voxymobile.fabric.VoxyChunkBridge;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
abstract class ClientWorldMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void voxymobile$afterSetBlockState(BlockPos pos, BlockState state, int flags,
                                               int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            VoxyChunkBridge.onBlockChanged((ClientWorld) (Object) this, pos, state);
        }
    }
}
