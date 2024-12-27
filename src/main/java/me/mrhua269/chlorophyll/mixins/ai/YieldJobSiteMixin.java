package me.mrhua269.chlorophyll.mixins.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.YieldJobSite;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(YieldJobSite.class)
public class YieldJobSiteMixin {
    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public static BehaviorControl<Villager> create(float f) {
        return BehaviorBuilder.create((instance) -> instance.group(instance.present(MemoryModuleType.POTENTIAL_JOB_SITE), instance.absent(MemoryModuleType.JOB_SITE), instance.present(MemoryModuleType.NEAREST_LIVING_ENTITIES), instance.registered(MemoryModuleType.WALK_TARGET), instance.registered(MemoryModuleType.LOOK_TARGET)).apply(instance, (memoryAccessor, memoryAccessor2, memoryAccessor3, memoryAccessor4, memoryAccessor5) -> (serverLevel, villager, l) -> {
            if (villager.isBaby()) {
                return false;
            } else if (villager.getVillagerData().getProfession() != VillagerProfession.NONE) {
                return false;
            } else {
                final GlobalPos globalPos = instance.get(memoryAccessor);
                final net.minecraft.server.level.ServerLevel targetLevel = serverLevel.getServer().getLevel(globalPos.dimension());
                BlockPos blockPos = globalPos.pos();
                if (targetLevel != serverLevel) return true;

                Optional<Holder<PoiType>> optional = serverLevel.getPoiManager().getType(blockPos);

                optional.flatMap(poiTypeHolder -> (instance.get(memoryAccessor3)).stream().filter((livingEntity) -> livingEntity instanceof Villager && livingEntity != villager).map((livingEntity) -> (Villager) livingEntity).filter(LivingEntity::isAlive).filter((villagerx) -> YieldJobSite.nearbyWantsJobsite(poiTypeHolder, villagerx, blockPos)).findFirst()).ifPresent((villagerx) -> {
                    memoryAccessor4.erase();
                    memoryAccessor5.erase();
                    memoryAccessor.erase();
                    if (villagerx.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()) {
                        BehaviorUtils.setWalkAndLookTargetMemories(villagerx, blockPos, f, 1);
                        villagerx.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(serverLevel.dimension(), blockPos));
                        DebugPackets.sendPoiTicketCountPacket(serverLevel, blockPos);
                    }

                });

                return true;
            }
        }));
    }
}
