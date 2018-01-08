package com.exensa.opendatastructures;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.Serializable;

/**
 * Created by Guillaume Pitel on 22/12/2017.
 * An approximate Hash Set containing long hashes
 * This is a open addressing linear hash set of fingerprints
 * Expansion is normally handled neatly by doubling the block lists, interleaving new empty block with old ones,
 * then moving old fingerprint to their new location
 * Because we don't use modulo but division for hash to position computation, locality is well preserved, thus
 * exmpansion should be optimal
 */


public class RobinHoodHashSetBucket implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int BLOCK_SIZE_SHIFT = 16;
    private static final long BLOCK_MASK = (1L << (BLOCK_SIZE_SHIFT)) - 1;
    private static final int BLOCK_SIZE = 1 << BLOCK_SIZE_SHIFT;
    // The blocks storing the fingerprints
    private ObjectArrayList<long[]> blocks;
    private int bucketFactor = 0;
    private long bucketMask = (1L << (BLOCK_SIZE_SHIFT)) - 1;
    private long bucketShiftFactor = (64-(BLOCK_SIZE_SHIFT + bucketFactor));
    private int blockIdMask = 1;
    private long half_size = BLOCK_SIZE >> 1;
    private long used = 0;
    private double threshold = 0.75;
    private long maxFill = Math.min((long)(2* half_size * threshold),2* half_size - 1);
    public long totalDistance = 0;
    public long nbHit = 0;
    public RobinHoodHashSetBucket() {
        this(0.5f);
    }

    public RobinHoodHashSetBucket(float t) {
        this(BLOCK_SIZE, t);
    }

    public RobinHoodHashSetBucket(long capacity, float t) {
        capacity = Math.max((long)(capacity/(double)t),BLOCK_SIZE);
        System.out.println("Capacity before rounding : " + capacity);
        if (Long.bitCount(capacity) > 1)
            capacity = Long.highestOneBit(capacity) << 1; // Take the upper power-of-two value, for instance 1000 => 1024
        System.out.println("Capacity after rounding : " + capacity);
        half_size = capacity >> 1;
        int nbBlocks = (int)(capacity >> BLOCK_SIZE_SHIFT); // compute the number of blocks
        blocks = new ObjectArrayList<long[]>(nbBlocks+1);
        for(int i=0;i<nbBlocks;i++) blocks.add(newBlock());
        bucketMask = (capacity << 1) - 1;
        maxFill = Math.min((int)(threshold * 2* half_size), 2* half_size - 1);
        bucketFactor = (Long.bitCount(capacity - 1 )) - BLOCK_SIZE_SHIFT  ;
        bucketShiftFactor = (64-(BLOCK_SIZE_SHIFT + bucketFactor));
        System.out.println("Capacity " + String.format("0x%X",capacity));
        System.out.println("MaxFill " + maxFill);
        System.out.println("Half Size " + String.format("0x%X",half_size));
        System.out.println("BucketFactor " + String.format("%X",bucketFactor));
        System.out.println("bucketMask " + String.format("%X",bucketMask));
        System.out.println("END " + String.format("%X",(Long.MAX_VALUE >> bucketShiftFactor) + (half_size) ));
        threshold = t;
    }

    private long[] newBlock() {
        long[] newBlock = new long[BLOCK_SIZE];
        for (int i = 0; i<BLOCK_SIZE;i++) newBlock[i] = Long.MAX_VALUE;
        return newBlock;
    }


    public boolean add(long fingerprint) {
        if (fingerprint == Long.MAX_VALUE) // MAX_VALUE forbidden
            fingerprint = 0;
        long position = (fingerprint >> bucketShiftFactor) + half_size;
        //System.out.println("Inserting " + fingerprint + " from position " + position);
        int blockId = (int)(position >>> (BLOCK_SIZE_SHIFT));
        int inBlockOffset = (int) (position & BLOCK_MASK);
        for (;blockId < blocks.size(); blockId++) {
            long [] block = blocks.get(blockId);


            for (;inBlockOffset < BLOCK_SIZE; inBlockOffset++) {
                long currentFingerprint = block[inBlockOffset];
                if (currentFingerprint < fingerprint)  // currentFingerPrint < fingerPrint
                    continue;

                if (currentFingerprint > fingerprint) { // currentFingerPrint > fingerPrint
                    block[inBlockOffset] = fingerprint;
                    if (currentFingerprint != Long.MAX_VALUE) {
                        fingerprint = currentFingerprint;
                        continue;
                    }
                    if (used++ > maxFill)
                        expand();
                    return true;
                }
                // The FP is already there
                return false;
            }
            inBlockOffset = 0;
        }
        // We are at the end of the world, need to create an overflow block !
        long[] newBlock = newBlock();
        newBlock[0] = fingerprint;
        blocks.add(newBlock);
        used++;
        return false;
    }

    public boolean contains(long fingerprint) {
        if (fingerprint == Long.MAX_VALUE) // MAX_VALUE forbidden
            fingerprint = 0;
        long position = (fingerprint >> bucketShiftFactor) + half_size;
        int blockId = (int)(position >>> (BLOCK_SIZE_SHIFT));
        int inBlockOffset = (int) (position & BLOCK_MASK);
            for (; blockId < blocks.size(); blockId++) {
                long[] block = blocks.get(blockId);
                for (; inBlockOffset < BLOCK_SIZE; inBlockOffset++) {
                    long currentFingerprint = block[inBlockOffset];
                    if (currentFingerprint >= fingerprint) {
                        return (currentFingerprint == fingerprint);
                    }
                }
                inBlockOffset = 0;
            }
            return false;
    }

    private long getAt(long i) {
        long[] block = blocks.get((int)(i >>> BLOCK_SIZE_SHIFT));
        int inBlockOffset = (int) (i & BLOCK_MASK);
        return block[inBlockOffset];
    }

    public boolean checkOrdered() {
        /*
        System.out.println("BLOCK_SIZE " + BLOCK_SIZE);
        System.out.println("BLOCK_MASK " + String.format("%X",BLOCK_MASK));
        System.out.println("bucketMask " + String.format("%X",bucketMask));
        System.out.println("END " + String.format("%X",(Long.MAX_VALUE >> bucketShiftFactor) + (half_size >> 1) ));
        */
        long last = Long.MIN_VALUE;
        long good = 0;
        long bad = 0;
        for(long i = 0; i < BLOCK_SIZE*blocks.size(); i ++) {
            long n = getAt(i);
            if (n != Long.MAX_VALUE) {
                if (n >= last)
                    good++;
                else {
                    System.out.println("Bad at " + i);
                    bad++;
                }
                last = n;
            }
        }
        System.out.println("Used/Size : " + used + "/" + 2* half_size);
        System.out.println("Good : " + good + "; Bad : " + bad);
        if (bad > 0)
            return false;
        return true;
    }

    // Insert a fingerprint without checks nor expansion - used in expand()
    private void _add(long fingerprint) {
        long position = (fingerprint >> bucketShiftFactor) + half_size;
        int blockId = (int)(position >>> (BLOCK_SIZE_SHIFT));
        int inBlockOffset = (int) (position & BLOCK_MASK);
        for (;blockId < blocks.size(); blockId++) {
            long[] block = blocks.get(blockId);
            for (; inBlockOffset < BLOCK_SIZE; inBlockOffset++) {
                long currentFingerprint = block[inBlockOffset];
                if (currentFingerprint > fingerprint) {
                    block[inBlockOffset] = fingerprint;
                    if (currentFingerprint == Long.MAX_VALUE)
                        return;
                    _add(currentFingerprint);
                    return;
                }
            }
            inBlockOffset = 0;
        }
        // We are at the end of the world, need to create an overflow block !
        long[] newBlock = newBlock();
        newBlock[0] = fingerprint;
        blocks.add(newBlock);
        return;
    }

    private void expand() {
        // Locking mechanism must be handled at another level
        // First we allocate a new block for each existing block
        int s = (int)((2L*half_size) >> BLOCK_SIZE_SHIFT);
        //checkOrdered();
        ObjectArrayList<long[]> newBlocks = new ObjectArrayList<long[]>(2*s);
        int i = 0;
        for (; i < s; i++) {
            newBlocks.add(blocks.get(i));
            newBlocks.add(newBlock());
        }
        ObjectArrayList<long[]> oldBlocks = blocks;

        blocks = newBlocks;
        half_size = 2* half_size;
        maxFill = Math.min((int)(threshold * 2* half_size), 2* half_size - 1);
        bucketFactor++;
        bucketShiftFactor = (64-(BLOCK_SIZE_SHIFT + bucketFactor));
        bucketMask = (bucketMask << 1) | 1;
        blockIdMask = blocks.size() - 1;
        long[] tmpBlock = newBlock();

        // Now do the actual reallocation
        for(i = 0; i < blocks.size(); i+=2) {
            long[] swapTmpBlock = blocks.get(i);
            blocks.set(i, tmpBlock);
            tmpBlock = swapTmpBlock;
            for (int j=0; j< BLOCK_SIZE;j++) {
                if (tmpBlock[j] != Long.MAX_VALUE)
                    _add(tmpBlock[j]);
                tmpBlock[j] = Long.MAX_VALUE;
            }
        }
        // Process old overflow blocks
        for(i = s; i < oldBlocks.size();i++) {
            tmpBlock = oldBlocks.get(i);
            for (int j = 0; j < BLOCK_SIZE; j++) {
                if (tmpBlock[j] != Long.MAX_VALUE)
                    _add(tmpBlock[j]);
                tmpBlock[j] = Long.MAX_VALUE;
            }
        }
        //checkOrdered();
    }
}
