package com.wangdeng.fastermc.mixin;

import com.google.common.collect.Lists;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.StructureSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorAccess {
    @Accessor
    BiomeSource getBiomeSource();

    @Accessor
    StructureSettings getSettings();

    @Accessor
    long getStrongholdSeed();

    @Accessor
    List<ChunkPos> getStrongholdPositions();
}
