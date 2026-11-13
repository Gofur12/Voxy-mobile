package dev.brezze.voxymobile.scheduler;

/** Completion delivered only when its revision is still current. */
public record MeshResult<T>(ChunkPos position, long revision, T mesh, Throwable error) {
    public boolean succeeded() { return error == null; }
}
