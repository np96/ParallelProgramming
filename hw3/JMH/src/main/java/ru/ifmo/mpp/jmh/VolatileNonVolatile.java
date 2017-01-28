package ru.ifmo.mpp.jmh;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(5)
@Warmup(iterations = 1)
@Measurement(iterations = 2)
public class VolatileNonVolatile {

    @Threads(2)
    static public class VolatileNonVolatile2Threads extends VolatileNonVolatile {
    }

    @Threads(4)
    static public class VolatileNonVolatile4Threads extends VolatileNonVolatile {
    }

    @Threads(8)
    static public class VolatileNonVolatile8Threads extends VolatileNonVolatile {
    }

    @Threads(16)
    static public class VolatileNonVolatile16Threads extends VolatileNonVolatile {

    }

    @Threads(32)
    static public class VolatileNonVolatile32Threads extends VolatileNonVolatile {
    }


    static public class Holder {
        public int nonVol;
        public volatile int vol;
    }


    @State(Scope.Thread)
    static public class ThreadHolder extends Holder {
    }

    @State(Scope.Benchmark)
    static public class BenchHolder extends Holder {
    }

    @Benchmark
    @Group("NonSharedNonVol")
    public void incrNonSharedNonVol(ThreadHolder hold) {
        hold.nonVol++;
    }

    @Benchmark
    @Group("NonSharedNonVol")
    public int getNonSharedNonVol(ThreadHolder hold) {
        return hold.nonVol;
    }

    @Benchmark
    @Group("SharedNonVol")
    public void incrSharedNonVol(BenchHolder hold) {
        hold.nonVol++;
    }

    @Benchmark
    @Group("SharedNonVol")
    public int getSharedNonVol(BenchHolder hold) {
        return hold.nonVol;
    }


    @Benchmark
    @Group("NonSharedVol")
    public void incrNonSharedVol(ThreadHolder hold) {
        hold.vol++;
    }

    @Benchmark
    @Group("NonSharedVol")
    public int getNonSharedVol(ThreadHolder hold) {
        return hold.vol;
    }

    @Benchmark
    @Group("SharedVol")
    public void incrSharedVol(BenchHolder hold) {
        hold.vol++;
    }

    @Benchmark
    @Group("SharedVol")
    public int getSharedVol(BenchHolder hold) {
        return hold.vol;
    }

}
