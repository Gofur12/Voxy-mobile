package dev.brezze.voxymobile.lod;

/** Supplies world-space terrain height at integer block coordinates. */
@FunctionalInterface
public interface HeightSampler {
    float heightAt(int worldBlockX, int worldBlockZ);
}
