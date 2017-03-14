package cachesim.Cache;

public class CacheAccessParameters {

    final public long address;
    final public int offset;
    final public int setIndex;
    final public long tag;

    public CacheAccessParameters(long address, int offset, int setIndex, long tag) {
        this.address = address;
        this.offset = offset;
        this.setIndex = setIndex;
        this.tag = tag;
    }
}
