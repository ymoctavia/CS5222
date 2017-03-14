package cachesim.Cache;

import cachesim.PLRU.AbstractPLRU;

import java.util.LinkedHashMap;

public class Cache {

    protected LinkedHashMap<Integer, CacheBlock>[] cache;

    private Stats stats;
    private int numberOfOffsetBits;
    private int numberOfIndexBits;
    private int addressSize;
    private int numberOfTagBits;
    private int numberOfSets;
    private AbstractPLRU PLRU;

    public Cache(int addressSize, int cacheSize, int blockSize, int ways, AbstractPLRU PLRU) {

        this.addressSize = addressSize;

        int numberOfCacheBlocks = cacheSize / blockSize;
        this.numberOfSets = numberOfCacheBlocks / ways;

        this.numberOfOffsetBits = log2(blockSize);
        this.numberOfIndexBits = log2(numberOfSets);
        this.numberOfTagBits = addressSize - numberOfOffsetBits - numberOfIndexBits;

        this.cache = new LinkedHashMap[numberOfSets];
        for (int i = 0; i < numberOfSets; i++) {
            this.cache[i] = new LinkedHashMap<>(blockSize, 1, true);

            for (int j = 0; j < blockSize; j++) {
                this.cache[i].put(j, new CacheBlock(0, false, false, false));
            }
        }

        this.stats = new Stats();
        this.PLRU = PLRU;
    }

    private int log2(int n) {
        return (int) (Math.log(n) / Math.log(2));
    }


    public CacheAccessParameters getCacheAccessParameters(long address) {

        int offset = (int) (address & ((1 << numberOfOffsetBits) - 1)); // First numberOfOffsetBits bits of address.
        int setIndex = (int) ((address >> numberOfOffsetBits) & ((1 << numberOfIndexBits) - 1)); // First numberOfIndexBits bits of address after first numberOfOffsetBits bits.
        long tag = address >> (addressSize - numberOfTagBits); // Last numberOfTagBits bits of address.

        return new CacheAccessParameters(address, offset, setIndex, tag);
    }

    public void actionsOnMiss(CacheAccessParameters cacheAccessParameters) {

    }


    public void actionsOnHit(CacheAccessParameters cacheAccessParameters) {
        stats.totalHits++;
    }

    public CacheBlock getBlock(CacheAccessParameters cacheAccessParameters) {

        if (PLRU != null) {
            PLRU.onGetBlock(cacheAccessParameters.setIndex, cacheAccessParameters.offset);
        }

        stats.totalAccesses++;
        return cache[cacheAccessParameters.setIndex].get(cacheAccessParameters.offset);

    }

    public void setBlock(CacheAccessParameters cacheAccessParameters, long newTag) {

        int victimBlockOffset;

        if (PLRU == null) {
            victimBlockOffset = cacheAccessParameters.offset;
        } else {
            victimBlockOffset = PLRU.getVictimBlockOffset(cacheAccessParameters.setIndex);
        }

        CacheBlock currentBlock = cache[cacheAccessParameters.setIndex].get(victimBlockOffset);
        currentBlock.valid = true;
        currentBlock.dirty = false;
        currentBlock.tag = newTag;

    }

    public void printStats() {
        stats.print();
    }
}