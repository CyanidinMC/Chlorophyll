package me.mrhua269.chlorophyll.mixins;

import com.google.gson.JsonArray;
import me.mrhua269.chlorophyll.Chlorophyll;
import me.mrhua269.chlorophyll.impl.ChlorophyllLevelTickLoop;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
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
        Chlorophyll.getTickLoop(serverPlayer.serverLevel()).addConnection(connection);
    }

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public ServerPlayer respawn(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason) {
        this.players.remove(serverPlayer);
        final ChlorophyllLevelTickLoop oldScope = Chlorophyll.getTickLoop(serverPlayer.serverLevel());
        serverPlayer.serverLevel().removePlayerImmediately(serverPlayer, removalReason);
        oldScope.removeConnection(serverPlayer.connection.connection);
        DimensionTransition dimensionTransition = serverPlayer.findRespawnPositionAndUseSpawnBlock(bl, DimensionTransition.DO_NOTHING);
        ServerLevel serverLevel = dimensionTransition.newLevel();
        final ChlorophyllLevelTickLoop newScope = Chlorophyll.getTickLoop(serverLevel);
        ServerPlayer serverPlayer2 = new ServerPlayer(this.server, serverLevel, serverPlayer.getGameProfile(), serverPlayer.clientInformation());
        serverPlayer2.connection = serverPlayer.connection;
        serverPlayer2.restoreFrom(serverPlayer, bl);
        serverPlayer2.setId(serverPlayer.getId());
        serverPlayer2.setMainArm(serverPlayer.getMainArm());
        if (!dimensionTransition.missingRespawnBlock()) {
            serverPlayer2.copyRespawnPosition(serverPlayer);
        }

        for (String string : serverPlayer.getTags()) {
            serverPlayer2.addTag(string);
        }

        Vec3 vec3 = dimensionTransition.pos();
        serverPlayer2.moveTo(vec3.x, vec3.y, vec3.z, dimensionTransition.yRot(), dimensionTransition.xRot());
        if (dimensionTransition.missingRespawnBlock()) {
            serverPlayer2.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }

        byte b = (byte) (bl ? 1 : 0);
        ServerLevel serverLevel2 = serverPlayer2.serverLevel();
        LevelData levelData = serverLevel2.getLevelData();
        serverPlayer2.connection.send(new ClientboundRespawnPacket(serverPlayer2.createCommonSpawnInfo(serverLevel2), (byte)b));
        serverPlayer2.connection.teleport(serverPlayer2.getX(), serverPlayer2.getY(), serverPlayer2.getZ(), serverPlayer2.getYRot(), serverPlayer2.getXRot());
        serverPlayer2.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverLevel.getSharedSpawnPos(), serverLevel.getSharedSpawnAngle()));
        serverPlayer2.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        serverPlayer2.connection.send(new ClientboundSetExperiencePacket(serverPlayer2.experienceProgress, serverPlayer2.totalExperience, serverPlayer2.experienceLevel));
        this.sendActivePlayerEffects(serverPlayer2);

        newScope.schedule(() -> {
            newScope.addConnection(serverPlayer2.connection.connection);
            this.sendLevelInfo(serverPlayer2, serverLevel);
            this.sendPlayerPermissionLevel(serverPlayer2);
            serverLevel.addRespawnedPlayer(serverPlayer2);
            this.players.add(serverPlayer2);
            this.playersByUUID.put(serverPlayer2.getUUID(), serverPlayer2);
            serverPlayer2.initInventoryMenu();
            serverPlayer2.setHealth(serverPlayer2.getHealth());
            if (!bl) {
                BlockPos blockPos = BlockPos.containing(dimensionTransition.pos());
                BlockState blockState = serverLevel.getBlockState(blockPos);
                if (blockState.is(Blocks.RESPAWN_ANCHOR)) {
                    serverPlayer2.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1.0F, 1.0F, serverLevel.getRandom().nextLong()));
                }
            }
        });

        return serverPlayer2;
    }
}
