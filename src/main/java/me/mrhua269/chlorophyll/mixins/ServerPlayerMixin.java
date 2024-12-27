package me.mrhua269.chlorophyll.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    @Shadow @Nullable public abstract BlockPos getRespawnPosition();

    @Shadow public abstract float getRespawnAngle();

    @Shadow public abstract boolean isRespawnForced();

    @Shadow public abstract ResourceKey<Level> getRespawnDimension();

    @Shadow
    private static Optional<ServerPlayer.RespawnPosAngle> findRespawnAndUseSpawnBlock(ServerLevel serverLevel, BlockPos blockPos, float f, boolean bl, boolean bl2) {
        return Optional.empty();
    }

    @Shadow @Final public MinecraftServer server;

    public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public TeleportTransition findRespawnPositionAndUseSpawnBlock(boolean bl, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        BlockPos blockPos = this.getRespawnPosition();
        float f = this.getRespawnAngle();
        boolean bl2 = this.isRespawnForced();
        ServerLevel serverLevel = this.server.getLevel(this.getRespawnDimension());

        if (serverLevel != null && blockPos != null) {
            Optional<ServerPlayer.RespawnPosAngle> optional = CompletableFuture.supplyAsync(() -> findRespawnAndUseSpawnBlock(serverLevel, blockPos, f, bl2, bl), this.server.overworld().getChunkSource().mainThreadProcessor).join();

            if (optional.isPresent()) {
                ServerPlayer.RespawnPosAngle respawnPosAngle = optional.get();
                return new TeleportTransition(serverLevel, respawnPosAngle.position(), Vec3.ZERO, respawnPosAngle.yaw(), 0.0F, postTeleportTransition);
            } else {
                return CompletableFuture.supplyAsync(() -> TeleportTransition.missingRespawnBlock(this.server.overworld(), this, postTeleportTransition), this.server.overworld().getChunkSource().mainThreadProcessor).join();
            }
        } else {
            return new TeleportTransition(this.server.overworld(), this, postTeleportTransition);
        }
    }
}
