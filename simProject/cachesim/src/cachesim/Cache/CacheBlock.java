package cachesim.Cache;

public class CacheBlock {

    public long tag;
    public boolean valid;
    public boolean dirty;
    public boolean prefetched;

    public CacheBlock(long tag, boolean valid, boolean dirty, boolean prefetched) {
        this.tag = tag;
        this.valid = valid;
        this.dirty = dirty;
        this.prefetched = prefetched;
    }
}
