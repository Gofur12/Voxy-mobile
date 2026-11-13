package dev.brezze.voxymobile.scheduler;

/** Immutable horizontal chunk coordinate. */
public record ChunkPos(int x, int z) {
    public long packed() { return ((long) x << 32) ^ (z & 0xffffffffL); }
    public long distanceSquared(int centerX, int centerZ) {
        long dx = (long) x - centerX;
        long dz = (long) z - centerZ;
        return dx * dx + dz * dz;
    }
}
