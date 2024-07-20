package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.CompletableFuture;

@Mixin(EndPortalBlock.class)
public class EndPortalMixin {
    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public DimensionTransition getPortalDestination(@NotNull ServerLevel serverLevel, Entity entity, BlockPos blockPos) {
        ResourceKey<Level> resourceKey = serverLevel.dimension() == Level.END ? Level.OVERWORLD : Level.END;
        ServerLevel serverLevel2 = serverLevel.getServer().getLevel(resourceKey);
        if (serverLevel2 == null) {
            return null;
        } else {
            boolean bl = resourceKey == Level.END;
            BlockPos blockPos2 = bl ? ServerLevel.END_SPAWN_POINT : serverLevel2.getSharedSpawnPos();
            Vec3 vec3 = blockPos2.getBottomCenter();
            float f = entity.getYRot();
            if (bl) {
                Vec3 finalVec = vec3;
                Chlorophyll.getTickLoop(serverLevel2).schedule(() -> EndPlatformFeature.createEndPlatform(serverLevel2, BlockPos.containing(finalVec).below(), true));
                f = Direction.WEST.toYRot();
                if (entity instanceof ServerPlayer) {
                    vec3 = vec3.subtract(0.0, 1.0, 0.0);
                }
            } else {
                if (entity instanceof ServerPlayer serverPlayer) {
                    return CompletableFuture.supplyAsync(() -> serverPlayer.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING), serverPlayer.serverLevel().getChunkSource().mainThreadProcessor).join();
                }

                vec3 = CompletableFuture.supplyAsync(() -> entity.adjustSpawnLocation(serverLevel2, blockPos2).getBottomCenter(), serverLevel2.getChunkSource().mainThreadProcessor).join();
            }

            return new DimensionTransition(serverLevel2, vec3, entity.getDeltaMovement(), f, entity.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
        }
    }
}

