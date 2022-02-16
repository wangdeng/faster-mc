package com.wangdeng.fastermc.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.wangdeng.fastermc.FasterMc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.BaseStoneSource;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StrongholdConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;
import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class MixinChunkGenerator {
    @Shadow
    protected BiomeSource biomeSource;
    @Shadow
    private StructureSettings settings;
    @Shadow
    private long strongholdSeed;
    @Shadow
    private List<ChunkPos> strongholdPositions = Lists.newArrayList();

    /**
     * //TODO优化算法
     */
    @Inject(method = "generateStrongholds", at = @At(value = "HEAD"), cancellable = true)
    private void generateStrongholds(CallbackInfo ci) {
        if (this.strongholdPositions.isEmpty()) {
            FasterMc.LOGGER.info("generateStrongholds replaced");
            long t1=System.currentTimeMillis();
            StrongholdConfiguration config = this.settings.stronghold();
            if (config != null && config.count() != 0) {
                Set<Biome> set = Sets.newHashSet();
                for (Biome biome : this.biomeSource.possibleBiomes()) {
                    //这块不费时间
                    if (biome.getGenerationSettings().isValidStart(StructureFeature.STRONGHOLD)) {
                        set.add(biome);
                    }
                }
                int distance = config.distance();
                int count = config.count();
                int spread = config.spread();
                Random random = new Random();
                random.setSeed(this.strongholdSeed);
                double d0 = random.nextDouble() * Math.PI * 2.0D;
                double cos0 = Math.cos(d0);
                double sin0 = Math.sin(d0);
                int j = 0;
                int k = 0;

                for (int l = 0; l < count; ++l) {
                    double d1 = (double) (4 * distance + distance * k * 6) + (random.nextDouble() - 0.5D) * (double) distance * 2.5D;
                    int i1 = (int) Math.round(cos0 * d1);
                    int j1 = (int) Math.round(sin0 * d1);

                    //这里需要优化掉
                    BlockPos blockpos = this.biomeSource.findBiomeHorizontal(SectionPos.sectionToBlockCoord(i1, 8), 0,
                            SectionPos.sectionToBlockCoord(j1, 8), 112, set::contains, random);
                    if (blockpos != null) {
                        i1 = SectionPos.blockToSectionCoord(blockpos.getX());
                        j1 = SectionPos.blockToSectionCoord(blockpos.getZ());
                    }

                    this.strongholdPositions.add(new ChunkPos(i1, j1));
                    d0 += (Math.PI * 2D) / (double) spread;
                    ++j;
                    if (j == spread) {
                        ++k;
                        j = 0;
                        spread = spread + 2 * spread / (k + 1);
                        spread = Math.min(spread, count - l);
                        d0 += random.nextDouble() * Math.PI * 2.0D;
                    }
                }
            }
            long t2=System.currentTimeMillis();
            FasterMc.LOGGER.info("128 stronghold's coordinate find use time:"+(t2-t1)/1000.0+"s");
        }
        ci.cancel();
    }
}
