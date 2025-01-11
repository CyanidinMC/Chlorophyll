package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow @Final private List<ServerPlayer> players;

    @Shadow @Final private MinecraftServer server;

    @Shadow public abstract void sendActivePlayerEffects(ServerPlayer serverPlayer);

    @Shadow public abstract void sendLevelInfo(ServerPlayer serverPlayer, ServerLevel serverLevel);

    @Shadow public abstract void sendPlayerPermissionLevel(ServerPlayer serverPlayer);

    @Shadow @Final private Map<UUID, ServerPlayer> playersByUUID;

    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V", shift = At.Shift.BEFORE))
    public void initConnectionListForPlayer(Connection connection, @NotNull ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci){
        ((ITaskSchedulingLevel) serverPlayer.serverLevel()).chlorophyll$getTickLoop().addConnection(connection);
    }

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public ServerPlayer respawn(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason) {
        this.players.remove(serverPlayer);
        serverPlayer.serverLevel().removePlayerImmediately(serverPlayer, removalReason);
        ((ITaskSchedulingLevel) serverPlayer.serverLevel()).chlorophyll$getTickLoop().removeConnection(serverPlayer.connection.connection);

        TeleportTransition teleportTransition = serverPlayer.findRespawnPositionAndUseSpawnBlock(!bl, TeleportTransition.DO_NOTHING);
        ServerLevel targetLevel = teleportTransition.newLevel();

        ServerPlayer serverPlayer2 = new ServerPlayer(this.server, targetLevel, serverPlayer.getGameProfile(), serverPlayer.clientInformation());
        serverPlayer2.connection = serverPlayer.connection;
        serverPlayer2.restoreFrom(serverPlayer, bl);
        serverPlayer2.setId(serverPlayer.getId());
        serverPlayer2.setMainArm(serverPlayer.getMainArm());

        if (!teleportTransition.missingRespawnBlock()) {
            serverPlayer2.copyRespawnPosition(serverPlayer);
        }

        for (String string : serverPlayer.getTags()) {
            serverPlayer2.addTag(string);
        }

        Vec3 vec3 = teleportTransition.position();
        serverPlayer2.moveTo(vec3.x, vec3.y, vec3.z, teleportTransition.yRot(), teleportTransition.xRot());
        if (teleportTransition.missingRespawnBlock()) {
            serverPlayer2.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }

        byte dataToKeep = (byte) (bl ? 1 : 0);
        ServerLevel serverLevel2 = serverPlayer2.serverLevel();
        LevelData levelData = serverLevel2.getLevelData();
        serverPlayer2.connection.send(new ClientboundRespawnPacket(serverPlayer2.createCommonSpawnInfo(serverLevel2), dataToKeep));
        serverPlayer2.connection.teleport(serverPlayer2.getX(), serverPlayer2.getY(), serverPlayer2.getZ(), serverPlayer2.getYRot(), serverPlayer2.getXRot());
        serverPlayer2.connection.send(new ClientboundSetDefaultSpawnPositionPacket(targetLevel.getSharedSpawnPos(), targetLevel.getSharedSpawnAngle()));
        serverPlayer2.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        serverPlayer2.connection.send(new ClientboundSetExperiencePacket(serverPlayer2.experienceProgress, serverPlayer2.totalExperience, serverPlayer2.experienceLevel));
        this.sendActivePlayerEffects(serverPlayer2);
        this.sendLevelInfo(serverPlayer2, targetLevel);
        this.sendPlayerPermissionLevel(serverPlayer2);
        this.players.add(serverPlayer2);
        this.playersByUUID.put(serverPlayer2.getUUID(), serverPlayer2);

        ((ITaskSchedulingLevel) targetLevel).chlorophyll$getTickLoop().schedule(() -> {
            ((ITaskSchedulingLevel) targetLevel).chlorophyll$getTickLoop().addConnection(serverPlayer2.connection.connection);
            targetLevel.addRespawnedPlayer(serverPlayer2);
            serverPlayer2.initInventoryMenu();
            serverPlayer2.setHealth(serverPlayer2.getHealth());
            BlockPos blockPos = serverPlayer2.getRespawnPosition();

            ServerLevel serverLevel3 = this.server.getLevel(serverPlayer2.getRespawnDimension());
            Runnable remaining = () -> {
                if (!bl && blockPos != null && serverLevel3 != null) {
                    BlockState blockState = serverLevel3.getBlockState(blockPos);
                    if (blockState.is(Blocks.RESPAWN_ANCHOR)) {
                        serverPlayer2.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1.0F, 1.0F, targetLevel.getRandom().nextLong()));
                    }
                }
            };

            if (serverLevel3 != targetLevel) {
                ((ITaskSchedulingLevel) serverLevel3).chlorophyll$getTickLoop().schedule(remaining);
                return;
            }

            remaining.run();
        });

        return serverPlayer2;
    }

    @Redirect(method = "disconnectAllPlayersWithProfile", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;disconnect(Lnet/minecraft/network/chat/Component;)V"))
    public void chlorophyll$disconnectAllPlayersWithProfile(ServerGamePacketListenerImpl serverGamePacketListenerImpl, Component component) {
        ((ITaskSchedulingLevel) serverGamePacketListenerImpl.player.serverLevel()).chlorophyll$getTickLoop().execute(() -> serverGamePacketListenerImpl.disconnect(component));
    }
}
