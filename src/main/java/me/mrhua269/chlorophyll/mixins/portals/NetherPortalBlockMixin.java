package me.mrhua269.chlorophyll.mixins.portals;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;

@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
    @Shadow @Nullable protected abstract TeleportTransition getExitPortal(ServerLevel serverLevel, Entity entity, BlockPos blockPos, BlockPos blockPos2, boolean bl, WorldBorder worldBorder);

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    @Nullable
    public TeleportTransition getPortalDestination(ServerLevel serverLevel, Entity entity, BlockPos blockPos) {
        ResourceKey<Level> resourceKey = serverLevel.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        ServerLevel serverLevel2 = serverLevel.getServer().getLevel(resourceKey);
        if (serverLevel2 == null) {
            return null;
        } else {
            return ((ITaskSchedulingLevel) serverLevel).chlorophyll$getTickLoop().spinWait(CompletableFuture.supplyAsync(() -> {
                boolean bl = serverLevel2.dimension() == Level.NETHER;
                WorldBorder worldBorder = serverLevel2.getWorldBorder();
                double d = DimensionType.getTeleportationScale(serverLevel.dimensionType(), serverLevel2.dimensionType());
                BlockPos blockPos2 = worldBorder.clampToBounds(entity.getX() * d, entity.getY(), entity.getZ() * d);
                return this.getExitPortal(serverLevel2, entity, blockPos, blockPos2, bl, worldBorder);
            }, serverLevel2.getChunkSource().mainThreadProcessor));
        }
    }
}
