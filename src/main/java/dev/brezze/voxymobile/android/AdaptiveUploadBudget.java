package dev.brezze.voxymobile.android;

/** Adjusts per-frame upload work using recent frame time; call once per rendered frame. */
public final class AdaptiveUploadBudget {
    private final int minUploads, maxUploads;
    private final long minBytes, maxBytes;
    private int uploads;
    private long bytes;
    public AdaptiveUploadBudget(int minUploads,int maxUploads,long minBytes,long maxBytes) {
        if(minUploads<1||maxUploads<minUploads||minBytes<1||maxBytes<minBytes) throw new IllegalArgumentException("invalid limits");
        this.minUploads=minUploads;this.maxUploads=maxUploads;this.minBytes=minBytes;this.maxBytes=maxBytes;
        uploads=minUploads;bytes=minBytes;
    }
    public synchronized void observeFrameMillis(double frameMillis) {
        if(!Double.isFinite(frameMillis)||frameMillis<0) return;
        if(frameMillis>22.0){uploads=Math.max(minUploads,uploads-1);bytes=Math.max(minBytes,bytes*3/4);}
        else if(frameMillis<15.0){uploads=Math.min(maxUploads,uploads+1);bytes=Math.min(maxBytes,bytes+Math.max(64L<<10,bytes/4));}
    }
    public synchronized int maxUploads(){return uploads;}
    public synchronized long maxBytes(){return bytes;}
}
