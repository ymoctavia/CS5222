package cachesim;

import cachesim.Cache.Cache;
import cachesim.Cache.CacheAccessParameters;
import cachesim.Cache.CacheBlock;
import cachesim.Cache.InstructionTypes;
import cachesim.PLRU.AbstractPLRU;
import cachesim.PLRU.BitBasedPLRU;
import cachesim.PLRU.TreeBasedPLRU;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Objects;

public class Main {

    private static HashMap<String, String> parseArgs(String[] args) {

        if (args.length < 2 || !Objects.equals(args[0], "-t")) {
            throw new IllegalArgumentException("Can't find -t argument");
        }

        HashMap<String, String> argsMap = new HashMap<>();

        if (args[1].length() == 0) {
            throw new IllegalArgumentException("-t argument value is empty");
        }
        argsMap.put("tracePath", args[1]);

        for (int i = 2; i <= 4; i += 2) {
            if (args.length <= i)
            {
                break;
            }
            else {
                switch (args[i].toLowerCase()) {
                    case "-l":
                        argsMap.put("tracingCountLimit", args[i + 1]);
                        break;
                    case "-plru":
                        String PLRUType = args[i + 1].toLowerCase();
                        if (PLRUType.length() == 0) {
                            throw new IllegalArgumentException("-plru argument value is empty");
                        }

                        argsMap.put("PLRUType", PLRUType);
                        break;
                }
            }
        }

        return argsMap;
    }

    private static AbstractPLRU getPLRU(String PLRUType, int targetCacheSize, int targetCacheBlockSize, int targetCacheWays, int wordSize) {

        AbstractPLRU PLRU;

        if (Objects.equals(PLRUType.toLowerCase(), "bitbased")) {
            PLRU = new BitBasedPLRU(targetCacheSize, targetCacheBlockSize, targetCacheWays, wordSize);
        } else if (Objects.equals(PLRUType.toLowerCase(), "treebased")) {
            PLRU = new TreeBasedPLRU(targetCacheSize, targetCacheBlockSize, targetCacheWays, wordSize);
        } else {
            throw new IllegalArgumentException("Can't determine PLRU type");
        }

        return PLRU;
    }

    public static void main(String[] args) throws IOException {

        HashMap<String, String> argsMap = new HashMap<>();

        try {
            argsMap = parseArgs(args);
        } catch (IllegalArgumentException exc) {
            System.out.println("Argument error: " + exc.getMessage());
            System.out.println("Usage: -t \"path_to_trace_file\" [-plru BitBased|TreeBased] [-l tracing_count_limit");
            System.exit(1); // Error exit code.
        }

        String tracePath = argsMap.get("tracePath");
        String PLRUType = argsMap.containsKey("PLRUType") ? argsMap.get("PLRUType") : "treebased";
        int tracingCountLimit = argsMap.containsKey("tracingCountLimit") ? Integer.parseInt(argsMap.get("tracingCountLimit")) : 0;

        System.out.println("\nStart trace of file: " + tracePath);

        int blockSize = 64; // Bytes.
        int ways = 8; // Associativity.
        int traceAddressSize = 64; // Bits.
        int L1CacheSize = 32 * 1024; // Bytes.
        int L2CacheSize = 256 * 1024; // Bytes.
        int wordSize = 8; // Bytes.

        AbstractPLRU L1PLRU = getPLRU(PLRUType, L1CacheSize, blockSize, ways, wordSize);
        AbstractPLRU L2PLRU = getPLRU(PLRUType, L2CacheSize, blockSize, ways, wordSize);

        Cache L1DataCache = new Cache(traceAddressSize, L1CacheSize, blockSize, ways, L1PLRU);
        Cache L1InstructionCache = new Cache(traceAddressSize, L1CacheSize, blockSize, ways, null);
        Cache victimCache = new Cache(traceAddressSize, blockSize * ways, blockSize, ways, null); // Fully assoc.
        Cache L2Cache = new Cache(traceAddressSize, L2CacheSize, blockSize, ways, L2PLRU);

        int count = 0;

        DataInputStream in = new DataInputStream(new FileInputStream(tracePath));
        try {
            while (in.available() > 0) {

                byte[] b = new byte[8];
                in.read(b);
                reverse(b); // Addresses written in little-endian order.

                InstructionTypes instructionType = null;

                if ((b[0] & 0xFF) == 0x00)
                    instructionType = InstructionTypes.LOAD;
                else if ((b[0] & 0xFF) == 0x80)
                    instructionType = InstructionTypes.STORE;
                else if ((b[0] & 0xFF) == 0x40)
                    instructionType = InstructionTypes.FETCH;
                else
                    continue;

                // Clear two highest bits after we inferred instruction type.
                b[0] &= 0x1F;

                ByteBuffer bb = ByteBuffer.wrap(b);
                long address = bb.getLong(); // As we clear highest bytes, we could use just long instead ulong.

                // Choose L1 cache.
                Cache L1Cache = (instructionType == InstructionTypes.FETCH) ? L1InstructionCache : L1DataCache;

                CacheAccessParameters L1cacheAccessParameters = L1Cache.getCacheAccessParameters(address);

                // First, check L1 for cache block.
                CacheBlock L1block = L1Cache.getBlock(L1cacheAccessParameters);

                boolean isL1Hit = false;
                boolean isL2Hit = false;
                boolean L1blockReplaced = false;

                // Block validation.
                if (L1block.valid && L1block.tag == L1cacheAccessParameters.tag) {
                    // L1 hit.

                    isL1Hit = true;

                } else {
                    // L1 miss.

                    // Check victim cache for cache block.
                    CacheAccessParameters victimCacheAccessParameters = victimCache.getCacheAccessParameters(address);

                    CacheBlock victimBlock = victimCache.getBlock(victimCacheAccessParameters);

                    if (victimBlock.valid && victimBlock.tag == victimCacheAccessParameters.tag) {
                        // Victim cache hit.
                        victimCache.actionsOnHit(victimCacheAccessParameters);

                        isL1Hit = true;

                        // Send hit block from victim to L1 - set L1 miss block as valid.
                        L1blockReplaced = true;

                        // Set ejected from L1 block to victim cache.
                        victimCache.setBlock(victimCacheAccessParameters, L1block.tag);

                        L1Cache.setBlock(L1cacheAccessParameters, L1cacheAccessParameters.tag);

                    } else {
                        // Victim cache miss.
                        L1Cache.actionsOnMiss(L1cacheAccessParameters);
                        victimCache.actionsOnMiss(victimCacheAccessParameters);
                    }
                }

                CacheAccessParameters L2cacheAccessParameters = null;

                if (isL1Hit) {
                    L1Cache.actionsOnHit(L1cacheAccessParameters);

                    if (instructionType == InstructionTypes.STORE) {
                        // L1 write-back strategy.
                        L1block.dirty = true;
                    }
                } else {

                    L2cacheAccessParameters = L2Cache.getCacheAccessParameters(address);

                    // Check L2.
                    CacheBlock L2block = L2Cache.getBlock(L2cacheAccessParameters);

                    if (L2block.valid && L2block.tag == L2cacheAccessParameters.tag) {
                        // L2 hit.
                        L2Cache.actionsOnHit(L2cacheAccessParameters);
                    } else {
                        // L2 miss.
                        L2Cache.actionsOnMiss(L2cacheAccessParameters);

                        // Send requested block from DRAM to L2 - set L2 missed block as valid.
                        L2Cache.setBlock(L2cacheAccessParameters, L2cacheAccessParameters.tag);
                    }

                    // Send requested block from L2 to L1 - set L1 missed block as valid.
                    L1blockReplaced = true;
                    L1Cache.setBlock(L1cacheAccessParameters, L1cacheAccessParameters.tag);

                }

                if (L1blockReplaced && L1block.dirty && L1block.valid) {
                    if (L2cacheAccessParameters == null) {
                        L2cacheAccessParameters = L2Cache.getCacheAccessParameters(address);
                    }
                    // L1 write-back strategy.
                    L2Cache.setBlock(L2cacheAccessParameters, L2cacheAccessParameters.tag);

                    // Write-through on L2 write.
                }

                count++;

                if (tracingCountLimit > 0 && count >= tracingCountLimit) {
                    break;
                }

            }

            System.out.println("\nTrace statistics:");
            System.out.println("\nRead " + count + " rows.");

            System.out.println("\nL1 data cache:");
            L1DataCache.printStats();

            System.out.println("\nL1 instruction cache:");
            L1InstructionCache.printStats();

            System.out.println("\nL2 cache:");
            L2Cache.printStats();

        } catch (EOFException ignored) {
            System.out.println("[EOF]");
        }
        in.close();

        System.out.println("\nPress ENTER to proceed..");

        System.in.read(new byte[1]);

    }

    public static void reverse(byte[] array) {
        if (array != null) {
            int i = 0;

            for (int j = array.length - 1; j > i; ++i) {
                byte tmp = array[j];
                array[j] = array[i];
                array[i] = tmp;
                --j;
            }
        }
    }
}