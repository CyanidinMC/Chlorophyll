package me.mrhua269.chlorophyll.mixins;

import com.mojang.authlib.GameProfile;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
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

    @Shadow public ServerGamePacketListenerImpl connection;

    @Shadow public abstract ServerLevel serverLevel();

    @Shadow public boolean isChangingDimension;

    @Shadow public abstract CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel serverLevel);

    @Shadow @Nullable public Vec3 enteredNetherPosition;

    @Shadow public abstract void setServerLevel(ServerLevel serverLevel);

    @Shadow public abstract void triggerDimensionChangeTriggers(ServerLevel serverLevel);

    @Shadow public int lastSentExp;

    @Shadow public float lastSentHealth;

    @Shadow public int lastSentFood;

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

    /**
     * @author MrHua69
     * @reason Worldized ticking
     */
    @Overwrite
    @Nullable
    public ServerPlayer teleport(TeleportTransition teleportTransition) {
        if (this.isRemoved()) {
            return null;
        } else {
            final ServerPlayer thisEntity = (ServerPlayer) (Object) this;
            if (teleportTransition.missingRespawnBlock()) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
            }

            ServerLevel serverLevel = teleportTransition.newLevel();
            ServerLevel serverLevel2 = this.serverLevel();
            ResourceKey<Level> resourceKey = serverLevel2.dimension();
            if (!teleportTransition.asPassenger()) {
                this.stopRiding();
            }

            if (serverLevel.dimension() == resourceKey) {
                this.connection.teleport(PositionMoveRotation.of(teleportTransition), teleportTransition.relatives());
                this.connection.resetPosition();
                teleportTransition.postTeleportTransition().onTransition(this);
            } else {
                this.isChangingDimension = true;
                LevelData levelData = serverLevel.getLevelData();
                this.connection.send(new ClientboundRespawnPacket(this.createCommonSpawnInfo(serverLevel), (byte)3));
                this.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
                PlayerList playerList = this.server.getPlayerList();
                playerList.sendPlayerPermissionLevel(thisEntity);
                serverLevel2.removePlayerImmediately(thisEntity, RemovalReason.CHANGED_DIMENSION);
                ((ITaskSchedulingLevel) serverLevel2).chlorophyll$getTickLoop().removeConnection(this.connection.connection);

                ((ITaskSchedulingLevel) serverLevel).chlorophyll$getTickLoop().schedule(() -> {
                    ((ITaskSchedulingLevel) serverLevel).chlorophyll$getTickLoop().addConnection(this.connection.connection);
                    this.unsetRemoved();

                    if (resourceKey == Level.OVERWORLD && serverLevel.dimension() == Level.NETHER) {
                        this.enteredNetherPosition = this.position();
                    }


                    this.setServerLevel(serverLevel);
                    this.connection.teleport(PositionMoveRotation.of(teleportTransition), teleportTransition.relatives());
                    this.connection.resetPosition();
                    serverLevel.addDuringTeleport(thisEntity);

                    this.triggerDimensionChangeTriggers(serverLevel2);
                    this.stopUsingItem();
                    this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                    playerList.sendLevelInfo(thisEntity, serverLevel);
                    playerList.sendAllPlayerInfo(thisEntity);
                    playerList.sendActivePlayerEffects(thisEntity);
                    teleportTransition.postTeleportTransition().onTransition(thisEntity);
                    this.lastSentExp = -1;
                    this.lastSentHealth = -1.0F;
                    this.lastSentFood = -1;
                });

            }
            return thisEntity;
        }
    }

}
