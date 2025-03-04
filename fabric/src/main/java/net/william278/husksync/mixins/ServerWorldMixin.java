/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.mixins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.william278.husksync.event.WorldSaveCallback;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Final
    @Shadow
    private MinecraftServer server;

    //#if MC==12104
    @Inject(method = "savePersistentState", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "saveLevel", at = @At("HEAD"))
    //#endif
    public void saveLevel(CallbackInfo ci) {
        if (server.isStopping() || server.isStopped()) {
            return;
        }
        WorldSaveCallback.EVENT.invoker().save((ServerWorld) (Object) this);
    }

}
