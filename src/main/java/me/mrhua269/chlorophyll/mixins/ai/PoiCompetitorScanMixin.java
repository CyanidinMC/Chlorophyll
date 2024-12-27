package me.mrhua269.chlorophyll.mixins.ai;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.PoiCompetitorScan;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PoiCompetitorScan.class)
public abstract class PoiCompetitorScanMixin {
    @Shadow
    private static boolean competesForSameJobsite(GlobalPos globalPos, Holder<PoiType> holder, Villager villager) {
        return false;
    }

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create((instance) -> instance.group(instance.present(MemoryModuleType.JOB_SITE), instance.present(MemoryModuleType.NEAREST_LIVING_ENTITIES)).apply(instance, (memoryAccessor, memoryAccessor2) -> (serverLevel, villager, l) -> {
            GlobalPos globalPos = instance.get(memoryAccessor);
            if (globalPos.dimension() != villager.level().dimension()) {
                return true;
            }
            serverLevel.getPoiManager().getType(globalPos.pos()).ifPresent((holder) -> (instance.get(memoryAccessor2)).stream()
                    .filter((livingEntity) -> livingEntity instanceof Villager && livingEntity != villager)
                    .map(entity -> (Villager)entity)
                    .filter(LivingEntity::isAlive)
                    .filter((villagerx) -> competesForSameJobsite(globalPos, holder, villagerx))
                    .reduce(villager, PoiCompetitorScan::selectWinner));
            return true;
        }));
    }
}
