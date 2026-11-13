package dev.brezze.voxymobile.render;

/** Immutable counters from one terrain render frame. */
public final class TerrainFrameStats {
    public final int uploads, candidates, visible, drawn;
    public TerrainFrameStats(int uploads, int candidates, int visible, int drawn) {
        this.uploads = uploads; this.candidates = candidates; this.visible = visible; this.drawn = drawn;
    }
}
