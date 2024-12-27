package me.mrhua269.chlorophyll.mixins.ai;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.GoToPotentialJobSite;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(GoToPotentialJobSite.class)
public class GoToPotentialJobSiteMixin {
    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public void stop(ServerLevel serverLevel, Villager villager, long l) {
        Optional<GlobalPos> optional = villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);

        optional.ifPresent((globalPos) -> {
            BlockPos blockPos = globalPos.pos();
            ServerLevel serverLevel2 = serverLevel.getServer().getLevel(globalPos.dimension());
            if (serverLevel2 != null) {
                Runnable scheduledRelease = () -> {
                    PoiManager poiManager = serverLevel2.getPoiManager();
                    if (poiManager.exists(blockPos, (holder) -> {
                        return true;
                    })) {
                        poiManager.release(blockPos);
                    }
                };

                if (serverLevel2 != serverLevel) {
                    ((ITaskSchedulingLevel) serverLevel2).chlorophyll$getTickLoop().schedule(scheduledRelease);
                }else {
                    scheduledRelease.run();
                }

                DebugPackets.sendPoiTicketCountPacket(serverLevel, blockPos);
            }
        });

        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
    }
}
