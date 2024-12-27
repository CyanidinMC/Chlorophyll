package me.mrhua269.chlorophyll.mixins.ai;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Function;

@Mixin(BabyFollowAdult.class)
public class BabyFollowAdultMixin {
    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public static OneShot<AgeableMob> create(UniformInt uniformInt, Function<LivingEntity, Float> function) {
        return BehaviorBuilder.create((instance) -> instance.group(instance.present(MemoryModuleType.NEAREST_VISIBLE_ADULT), instance.registered(MemoryModuleType.LOOK_TARGET), instance.absent(MemoryModuleType.WALK_TARGET)).apply(instance, (closetMemory, lookTargetMemory, walkTargetMemory) -> (serverLevel, self, l) -> {
            if (!self.isBaby()) {
                return false;
            } else {
                AgeableMob closetAdult = instance.get(closetMemory);

                if (closetAdult.level() != self.level()) {
                    lookTargetMemory.erase();
                    walkTargetMemory.erase();
                    return true;
                }

                if (self.closerThan(closetAdult, uniformInt.getMaxValue() + 1) && !self.closerThan(closetAdult, uniformInt.getMinValue())) {
                    WalkTarget walkTarget = new WalkTarget(new EntityTracker(closetAdult, false), function.apply(self), uniformInt.getMinValue() - 1);
                    lookTargetMemory.set(new EntityTracker(closetAdult, true));
                    walkTargetMemory.set(walkTarget);
                    return true;
                } else {
                    return false;
                }
            }
        }));
    }
}
