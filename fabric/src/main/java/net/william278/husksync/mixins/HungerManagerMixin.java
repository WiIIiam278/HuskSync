package net.william278.husksync.mixins;

import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HungerManager.class)
public class HungerManagerMixin {

    @Shadow
    private float exhaustion;

    @Accessor("exhaustion")
    public void setExhaustion(float exhaustion) {
        this.exhaustion = exhaustion;
    }

    @Accessor("exhaustion")
    public float getExhaustion() {
        return exhaustion;
    }

}
