package dev.brezze.voxymobile.lod;

/** A square terrain tile measured in Minecraft chunks. level 0 is one chunk. */
public final class LodTile {
    public final int chunkX, chunkZ, level, sizeChunks;

    public LodTile(int chunkX, int chunkZ, int level) {
        if (level < 0 || level > 20) throw new IllegalArgumentException("level must be 0..20");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.level = level;
        this.sizeChunks = 1 << level;
    }

    public long key() {
        long a = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
        return (a * 31L) ^ level;
    }

    @Override public String toString() {
        return "LodTile{" + chunkX + ',' + chunkZ + ", level=" + level + '}';
    }
}
