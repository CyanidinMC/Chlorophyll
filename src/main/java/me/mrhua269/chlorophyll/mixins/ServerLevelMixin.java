package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import me.mrhua269.chlorophyll.impl.ChlorophyllLevelTickLoop;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements ITaskSchedulingLevel {
    @Shadow public abstract PoiManager getPoiManager();

    @Unique
    private boolean setup = false;
    @Unique
    @Final
    private final ChlorophyllLevelTickLoop tickLoop = new ChlorophyllLevelTickLoop((ServerLevel) (Object)this, Chlorophyll.workerPool);

    @Override
    public ChlorophyllLevelTickLoop chlorophyll$getTickLoop() {
        return this.tickLoop;
    }

    @Override
    public void chlorophyll$setupTickLoop() {
        if (!this.setup) {
            Chlorophyll.logger.info("Crating tick loop for level {}", ((Level)(Object) this).dimension().location());
            this.tickLoop.schedule();
            this.setup = true;
        }
    }

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public void onBlockStateChange(BlockPos blockPos, BlockState blockState, BlockState blockState2) {
        Optional<Holder<PoiType>> optional = PoiTypes.forState(blockState);
        Optional<Holder<PoiType>> optional2 = PoiTypes.forState(blockState2);
        if (!Objects.equals(optional, optional2)) {
            BlockPos blockPos2 = blockPos.immutable();
            optional.ifPresent((holder) -> this.tickLoop.execute(() -> {
                this.getPoiManager().remove(blockPos2);
                DebugPackets.sendPoiRemovedPacket(((ServerLevel) (Object) this), blockPos2);
            }));
            optional2.ifPresent((holder) -> this.tickLoop.execute(() -> {
                this.getPoiManager().add(blockPos2, holder);
                DebugPackets.sendPoiRemovedPacket(((ServerLevel) (Object) this), blockPos2);
            }));
        }
    }

    @Redirect(method = "onStructureStartsAvailable", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;execute(Ljava/lang/Runnable;)V"))
    private void onStructureStartsAvailable$mainThreadReturn(MinecraftServer server, Runnable runnable) {
        this.tickLoop.execute(runnable);
    }
}
