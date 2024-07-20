package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    @Nullable
    public Entity changeDimension(DimensionTransition dimensionTransition) {
        final ServerPlayer thisEntity = (ServerPlayer) (Object) this;
        if (thisEntity.isRemoved()) {
            return null;
        } else {
            if (dimensionTransition.missingRespawnBlock()) {
                thisEntity.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
            }

            ServerLevel destinationLevel = dimensionTransition.newLevel();
            ServerLevel currentLevel = thisEntity.serverLevel();
            ResourceKey<Level> resourceKey = currentLevel.dimension();
            if (destinationLevel.dimension() == resourceKey) {
                thisEntity.connection.teleport(dimensionTransition.pos().x, dimensionTransition.pos().y, dimensionTransition.pos().z, dimensionTransition.yRot(), dimensionTransition.xRot());
                thisEntity.connection.resetPosition();
                dimensionTransition.postDimensionTransition().onTransition(thisEntity);
            } else {
                thisEntity.isChangingDimension = true;
                currentLevel.removePlayerImmediately(thisEntity, Entity.RemovalReason.CHANGED_DIMENSION);
                Chlorophyll.getTickLoop(currentLevel).removeConnection(thisEntity.connection.connection);

                Chlorophyll.getTickLoop(destinationLevel).schedule(() -> {
                    Chlorophyll.getTickLoop(destinationLevel).addConnection(thisEntity.connection.connection);
                    LevelData levelData = destinationLevel.getLevelData();
                    thisEntity.connection.send(new ClientboundRespawnPacket(thisEntity.createCommonSpawnInfo(destinationLevel), (byte)3));
                    thisEntity.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
                    PlayerList playerList = thisEntity.server.getPlayerList();
                    playerList.sendPlayerPermissionLevel(thisEntity);
                    thisEntity.unsetRemoved();
                    if (resourceKey == Level.OVERWORLD && destinationLevel.dimension() == Level.NETHER) {
                        thisEntity.enteredNetherPosition = thisEntity.position();
                    }

                    thisEntity.setServerLevel(destinationLevel);
                    thisEntity.connection.teleport(dimensionTransition.pos().x, dimensionTransition.pos().y, dimensionTransition.pos().z, dimensionTransition.yRot(), dimensionTransition.xRot());
                    thisEntity.connection.resetPosition();
                    destinationLevel.addDuringTeleport(thisEntity);
                    thisEntity.triggerDimensionChangeTriggers(currentLevel);
                    thisEntity.connection.send(new ClientboundPlayerAbilitiesPacket(thisEntity.getAbilities()));
                    playerList.sendLevelInfo(thisEntity, destinationLevel);
                    playerList.sendAllPlayerInfo(thisEntity);
                    playerList.sendActivePlayerEffects(thisEntity);
                    dimensionTransition.postDimensionTransition().onTransition(thisEntity);
                    thisEntity.lastSentExp = -1;
                    thisEntity.lastSentHealth = -1.0F;
                    thisEntity.lastSentFood = -1;
                });
            }

            return thisEntity;
        }
    }

}
