package dev.brezze.voxymobile.lod;

/** Optional frustum hook. Implementations must be cheap and side-effect free. */
public interface TileVisibility {
    boolean isVisible(int minBlockX, int minBlockZ, int maxBlockXExclusive, int maxBlockZExclusive);
}
