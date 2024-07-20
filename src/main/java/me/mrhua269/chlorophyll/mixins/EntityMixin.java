package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {
    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public @Nullable Entity changeDimension(DimensionTransition dimensionTransition) {
        final Entity thisEntity = (Entity) (Object) this;
        Level var3 = thisEntity.level();
        if (var3 instanceof ServerLevel currentLevel) {
            if (!thisEntity.isRemoved()) {
                ServerLevel destination = dimensionTransition.newLevel();
                List<Entity> list = thisEntity.getPassengers();
                thisEntity.unRide();
                List<Entity> list2 = new ArrayList<>();

                for (Entity entity : list) {
                    Entity entity2 = entity.changeDimension(dimensionTransition);
                    if (entity2 != null) {
                        list2.add(entity2);
                    }
                }

                Entity entity3 = destination.dimension() == currentLevel.dimension() ? thisEntity : thisEntity.getType().create(destination);
                if (entity3 != null) {
                    if (thisEntity != entity3) {
                        entity3.restoreFrom(thisEntity);
                        thisEntity.removeAfterChangingDimensions();
                    }

                    Chlorophyll.getTickLoop(destination).schedule(() -> {
                        entity3.moveTo(dimensionTransition.pos().x, dimensionTransition.pos().y, dimensionTransition.pos().z, dimensionTransition.yRot(), entity3.getXRot());
                        entity3.setDeltaMovement(dimensionTransition.speed());
                        if (thisEntity != entity3) {
                            destination.addDuringTeleport(entity3);
                        }

                        for (Entity entity : list2) {
                            entity.startRiding(entity3, true);
                        }

                        destination.resetEmptyTime();
                        dimensionTransition.postDimensionTransition().onTransition(entity3);
                    });
                    currentLevel.resetEmptyTime();
                }
                return entity3;
            }
        }

        return null;
    }
}
