package dev.brezze.voxymobile.render;

import dev.brezze.voxymobile.scheduler.ChunkPos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable per-frame chunk coordinate list; contains no Minecraft objects. */
public final class ChunkRenderList {
    private final List<ChunkPos> chunks;

    public ChunkRenderList(Iterable<ChunkPos> source) {
        if (source == null) throw new NullPointerException("source");
        ArrayList<ChunkPos> copy = new ArrayList<>();
        for (ChunkPos pos : source) {
            if (pos == null) continue;
            copy.add(pos);
        }
        this.chunks = Collections.unmodifiableList(copy);
    }

    public List<ChunkPos> chunks() { return chunks; }
    public int size() { return chunks.size(); }
}
