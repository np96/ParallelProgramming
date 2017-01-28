package ru.ifmo.mpp.jmh;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(5)
@Warmup(iterations = 1)
@Measurement(iterations = 2)
public class SyncMethods {


    @Threads(2)
    public static class SyncMethods2Threads extends SyncMethods {
    }

    @Threads(4)
    public static class SyncMethods4Threads extends SyncMethods {
    }

    @Threads(8)
    public static class SyncMethods8Threads extends SyncMethods {
    }

    @Threads(16)
    public static class SyncMethods16Threads extends SyncMethods {
    }

    @Threads(32)
    public static class SyncMethods32Threads extends SyncMethods {
    }


    public static class Volatile {
        public volatile long vol;
    }

    @State(Scope.Benchmark)
    public static class VolatileShared extends Volatile {

    }

    @State(Scope.Thread)
    public static class VolatileNonShared extends Volatile {
    }

    public static class Sync {

        long val;

        public synchronized long get() {
            return val;
        }

        public synchronized void incr() {
            val++;
        }

    }


    @State(Scope.Benchmark)
    public static class SyncShared extends Sync {
    }

    @State(Scope.Thread)
    public static class SyncNonShared extends Sync {
    }

    public static class Lock {
        private final ReentrantLock lock = new ReentrantLock();
        long val;

        public long get() {
            try {
                lock.lock();
                return val;
            } finally {
                lock.unlock();
            }
        }

        public void incr() {
            try {
                lock.lock();
                val++;
            } finally {
                lock.unlock();
            }
        }
    }


    @State(Scope.Benchmark)
    public static class LockShared extends Lock {
    }

    @State(Scope.Thread)
    public static class LockNonShared extends Lock {
    }


    @Benchmark
    @Group("SyncNonShared")
    public long getNonShared(SyncNonShared nonShared) {
        return nonShared.get();
    }

    @Benchmark
    @Group("SyncNonShared")
    public void incrNonShared(SyncNonShared nonShared) {
        nonShared.incr();
    }


    @Benchmark
    @Group("SyncShared")
    public long getShared(SyncShared shared) {
        return shared.get();
    }

    @Benchmark
    @Group("SyncShared")
    public void incrShared(SyncShared shared) {
        shared.incr();
    }

    @Benchmark
    @Group("LockNonShared")
    public long getNonShared(LockNonShared nonShared) {
        return nonShared.get();
    }

    @Benchmark
    @Group("LockNonShared")
    public void incrNonShared(LockNonShared nonShared) {
        nonShared.incr();
    }


    @Benchmark
    @Group("LockShared")
    public long getShared(LockShared shared) {
        return shared.get();
    }

    @Benchmark
    @Group("LockShared")
    public void incrShared(LockShared shared) {
        shared.incr();
    }

    @Benchmark
    @Group("VolNonShared")
    public long getNonShared(VolatileNonShared nonShared) {
        return nonShared.vol;
    }

    @Benchmark
    @Group("VolNonShared")
    public void incrNonShared(VolatileNonShared nonShared) {
        nonShared.vol++;
    }


    @Benchmark
    @Group("VolShared")
    public long getShared(VolatileShared shared) {
        return shared.vol;
    }

    @Benchmark
    @Group("VolShared")
    public void incrShared(VolatileShared shared) {
        shared.vol++;
    }
}
