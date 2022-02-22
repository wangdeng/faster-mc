package com.wangdeng.fastermc.mixin;

import com.google.common.collect.Sets;
import com.wangdeng.fastermc.FasterMc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StrongholdConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ChunkGenerator.class)
public abstract class MixinChunkGenerator {

    private static Map<String, List<ChunkPos>> strongholdPositionMap = new ConcurrentHashMap<>();


    /**
     * 优化末地要塞的坐标计算过程
     */
    @Inject(method = "generateStrongholds", at = @At(value = "HEAD"), cancellable = true)
    private void generateStrongholds(CallbackInfo ci) {
        ChunkGeneratorAccess access = (ChunkGeneratorAccess) (Object) this;
        List<ChunkPos> posList = access.getStrongholdPositions();
        if (posList.isEmpty()) {
            long seed = access.getStrongholdSeed();
            FasterMc.LOGGER.info("wordSeed is " + seed);
            StrongholdConfiguration config = access.getSettings().stronghold();
            if (config != null && config.count() != 0) {
                String key = new StringJoiner(":").add(seed + "").add(config.distance() + "").add(config.spread() + "").add(config.count() + "").toString();
                if (strongholdPositionMap.containsKey(key)) {
                    posList.addAll(strongholdPositionMap.get(key));
                    return;
                }
                Set<Biome> set = Sets.newHashSet();
                for (Biome biome : access.getBiomeSource().possibleBiomes()) {
                    if (biome.getGenerationSettings().isValidStart(StructureFeature.STRONGHOLD)) {
                        set.add(biome);
                    }
                }
                int distance = config.distance();
                int count = config.count();
                int spread = config.spread();
                Random random = new Random();
                random.setSeed(seed);
                double d0 = random.nextDouble() * Math.PI * 2.0D;
                int j = 0;
                int k = 0;
                BiomeSource source = access.getBiomeSource();
                //128
                for (int l = 0; l < count; ++l) {
                    double d1 = (double) (4 * distance + distance * k * 6) + (random.nextDouble() - 0.5D) * (double) distance * 2.5D;
                    int i1 = (int) Math.round( Math.cos(d0) * d1);
                    int j1 = (int) Math.round(Math.sin(d0) * d1);

                    //这里需要优化掉
                    BlockPos blockpos = this.findBiomeHorizontal(source, (i1 << 4) + 8, 0,
                            (j1 << 4) + 8, 112, set, random);
                    if (blockpos != null) {
                        i1 = blockpos.getX() >> 4;
                        j1 = blockpos.getZ() >> 4;
                    }

                    posList.add(new ChunkPos(i1, j1));
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
                if (seed != 0L) {
                    strongholdPositionMap.put(key, posList);
                }
            }
        }
        ci.cancel();
    }


    private BlockPos findBiomeHorizontal(BiomeSource biomeSource, int a, int b, int c, int d, Set<Biome> set, Random random) {
        int n = a >> 2;
        int o = c >> 2;
        int p = d >> 2;
        int q = b >> 2;
        int r = 0;
        int x = 0, z = 0;
        boolean flag = true;
        //448
        Biome biome;
        for (int i = -p; i <= p; i++) {
            //448
            for (int j = -p; j <= p; j++) {
                int bl3 = n + j;
                int w = o + i;
                biome = biomeSource.getNoiseBiome(bl3, q, w);
                if (set.contains(biome)) {
                    if (flag) {
                        flag = false;
                        x = bl3;
                        z = w;
                    } else if (random.nextInt(r + 1) == 0) {
                        x = bl3;
                        z = w;
                    }
                    ++r;
                }
            }
        }
        return flag ? null : new BlockPos(x << 2, b, z << 2);
    }
}
