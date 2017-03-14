package cachesim.PLRU;

public abstract class AbstractPLRU {

    private int numberOfSets;
    private int offset;

    public abstract int getVictimBlockOffset(int setIndex);

    public abstract void onGetBlock(int setIndex, int offset);

}
