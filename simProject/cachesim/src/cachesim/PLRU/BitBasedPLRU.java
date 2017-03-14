package cachesim.PLRU;

import java.util.BitSet;
import java.util.LinkedHashMap;

public class BitBasedPLRU extends AbstractPLRU {

    private LinkedHashMap<Integer, BitSet> sets;

    private int numberOfWordsInBlock;
    private int targetCacheWays;

    public BitBasedPLRU(int targetCacheSize, int targetCacheBlockSize, int targetCacheWays, int wordSize) {

        int numberOfCacheBlocks = targetCacheSize / targetCacheBlockSize;
        int numberOfSets = numberOfCacheBlocks / targetCacheWays;
        this.numberOfWordsInBlock = targetCacheBlockSize / wordSize;
        this.targetCacheWays = targetCacheWays;

        this.sets = new LinkedHashMap<>();

        // Create bit set of targetCacheWays bits for each set.
        for (int i = 0; i < numberOfSets; i++) {
            this.sets.put(i, new BitSet(targetCacheWays));
        }
    }

    public int getVictimBlockOffset(int setIndex) {
        // Get random (fixed left-to right, for simplicity) 0 bit.
        int indexOfClearBit = sets.get(setIndex).nextClearBit(0);

        onBitSetAccess(setIndex, indexOfClearBit);

        // Calculate first word of cache block corresponding to clear bit.
        return indexOfClearBit * numberOfWordsInBlock;
    }

    private void onBitSetAccess(int setIndex, int indexOfBit) {
        BitSet currentBitSet = sets.get(setIndex);

        // Set touched bit to 1.
        currentBitSet.set(indexOfBit);

        // If all other bits are 1, flip them to 0.
        int nextClearBit = currentBitSet.nextClearBit(0);
        if (nextClearBit == -1 || nextClearBit >= targetCacheWays) {
            currentBitSet.set(0, targetCacheWays, false);

            // Keep touched bit as 1.
            currentBitSet.set(indexOfBit);
        }
    }

    public void onGetBlock(int setIndex, int offset) {
        // Calculate touched bit from offset.
        int touchedBit = offset / numberOfWordsInBlock;
        onBitSetAccess(setIndex, touchedBit);
    }
}
