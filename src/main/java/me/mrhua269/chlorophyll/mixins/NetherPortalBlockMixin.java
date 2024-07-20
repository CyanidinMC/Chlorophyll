package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.CompletableFuture;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    @Nullable
    public DimensionTransition getPortalDestination(@NotNull ServerLevel serverLevel, Entity entity, BlockPos blockPos) {
        final NetherPortalBlock thisBlock = (NetherPortalBlock)(Object) this;
        ResourceKey<Level> resourceKey = serverLevel.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        ServerLevel serverLevel2 = serverLevel.getServer().getLevel(resourceKey);
        if (serverLevel2 == null) {
            return null;
        } else {
            boolean bl = serverLevel2.dimension() == Level.NETHER;
            WorldBorder worldBorder = serverLevel2.getWorldBorder();
            double d = DimensionType.getTeleportationScale(serverLevel.dimensionType(), serverLevel2.dimensionType());
            BlockPos blockPos2 = worldBorder.clampToBounds(entity.getX() * d, entity.getY(), entity.getZ() * d);
            return CompletableFuture.supplyAsync(() -> thisBlock.getExitPortal(serverLevel2, entity, blockPos, blockPos2, bl, worldBorder), serverLevel2.getChunkSource().mainThreadProcessor).join();
        }
    }
}
