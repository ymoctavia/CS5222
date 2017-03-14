package cachesim.Cache;

public class Stats {

    public long totalAccesses = 0;
    public long totalHits = 0;

    public void print() {

        double hitRate = totalHits / (double) totalAccesses;

        System.out.format("\tTotal hits: %d\n", totalHits);
        System.out.format("\tTotal misses: %d\n", totalAccesses - totalHits);
        System.out.format("\tTotal accesses: %d\n", totalAccesses);

        System.out.format("\tHit rate: %.2f%%\n", hitRate * 100.0);
        System.out.format("\tMiss-rate: %.2f%%\n", (1 - hitRate) * 100.0);

    }
}
