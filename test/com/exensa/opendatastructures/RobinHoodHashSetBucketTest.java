package com.exensa.opendatastructures;

import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongOpenHashBigSet;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import org.junit.Test;

import java.util.Random;
import static org.junit.Assert.*;

/**
 * Created by Guillaume Pitel on 03/01/2018.
 */
public class RobinHoodHashSetBucketTest {

    @Test
    public void testAdd() {
        int s = (int)(32*1024*1024*0.8);
        //TreeSet<Long> ts = new TreeSet<Long>();
        RobinHoodHashSetBucket b = new RobinHoodHashSetBucket(s,0.85f);
        LongOpenHashBigSet lohs = new LongOpenHashBigSet(s,0.85f);

        Random rng = new Random();

        long[] data = new long[s];
        long[] sorteddata = new long[s];
        long[] sdata = new long[s];
        long[] negdata = new long[s];
        for (int i =0; i < s; i++) {
            long v = rng.nextLong();
            data[i] = v;
            sdata[i] = v;
        }
        for (int i =0; i < s; i++) {
            long v = rng.nextLong();
            negdata[i] = v;
        }

        LongArrays.shuffle(sdata, new Random());

        System.out.println("Inserting " + s + " items in set");
        System.gc();
        long startLOHSTime = System.nanoTime();
        for (int i =0; i < s; i++)
            lohs.add(data[i]);
        long endLOHSTime = System.nanoTime();

        long startGetLOHSTime = System.nanoTime();
        for (int i =0; i < s; i++)
            lohs.contains(sdata[i]);
        System.gc();
        long endGetLOHSTime = System.nanoTime();

        long startNegGetLOHSTime = System.nanoTime();
        for (int i =0; i < s; i++)
            lohs.contains(negdata[i]);
        System.gc();
        long endNegGetLOHSTime = System.nanoTime();

        System.gc();
        long startRHHSTime = System.nanoTime();
        for (int i =0; i < s; i++)
            b.add(data[i]);
        long endRHHSTime = System.nanoTime();

        long startGetRHHSTime = System.nanoTime();
        for (int i =0; i < s; i++)
            b.contains(sdata[i]);
        System.gc();
        long endGetRHHSTime = System.nanoTime();

        long startNegGetRHHSTime = System.nanoTime();
        for (int i =0; i < s; i++)
            b.contains(negdata[i]);
        System.gc();
        long endNegGetRHHSTime = System.nanoTime();

        long startNativeSortTime = System.nanoTime();
        LongArrays.quickSort(data);
        //LongArrays.mergeSort(data);

        long endNativeSortTime = System.nanoTime();

        System.out.println("Average distance :" + (double)b.totalDistance / b.nbHit);
        System.out.println("Estimated RobinHoodHashSet object size : " + ObjectSizeCalculator.getObjectSize(b));
        System.out.println("Estimated LongOpenHastSet object size : " + ObjectSizeCalculator.getObjectSize(lohs));
        System.out.println("RHHS add total : " + (double)(endRHHSTime-startRHHSTime)/1000000000.0 + "s ; per item " + (double)(endRHHSTime-startRHHSTime)/s);
        System.out.println("LOHS add total : " + (double)(endLOHSTime-startLOHSTime)/1000000000.0 + "s ; per item " + (double)(endLOHSTime-startLOHSTime)/s);
        System.out.println("RHHS get per item " + (double)(endGetRHHSTime-startGetRHHSTime)/s);
        System.out.println("LOHS get per item " + (double)(endGetLOHSTime-startGetLOHSTime)/s);
        System.out.println("RHHS neg get per item " + (double)(endNegGetRHHSTime-startNegGetRHHSTime)/s);
        System.out.println("LOHS neg get per item " + (double)(endNegGetLOHSTime-startNegGetLOHSTime)/s);
        System.out.println("Native sort : " + (double)(endNativeSortTime-startNativeSortTime)/1000000000.0 + "s ");
        assertEquals(true,b.checkOrdered());
        for (long v:data) {
            assertEquals(true, b.contains(v));
        }
        for (long v:negdata) {
            assertEquals(false, b.contains(v));
        }

    }

}