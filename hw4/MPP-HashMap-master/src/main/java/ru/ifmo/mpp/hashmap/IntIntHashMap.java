package ru.ifmo.mpp.hashmap;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Int-to-Int hash map with open addressing and linear probes.
 * <p>
 * This class <b>IS</b> thread-safe.
 *
 * @author Поперечный.
 */
public class IntIntHashMap {
    private static final int MAGIC = 0x9E3779B9; // golden ratio
    private static final int INITIAL_CAPACITY = 2; // !!! DO NOT CHANGE INITIAL CAPACITY !!!
    private static final int MAX_PROBES = 8; // max number of probes to find an item

    private static final int NULL_KEY = 0; // missing key (initial value)
    private static final int NULL_VALUE = 0; // missing value (initial value)
    private static final int DEL_VALUE = Integer.MAX_VALUE; // mark for removed value
    private static final int DONE_VALUE = Integer.MIN_VALUE;
    private static final int NEEDS_REHASH = -1; // returned by putInternal to indicate that rehash is needed

    // Checks is the value is in the range of allowed values
    private static boolean isValue(int value) {
        return value > 0 && value < DEL_VALUE; // the range or allowed values
    }

    // Converts internal value to the public results of the methods
    private static int toValue(int value) {
        return isValue(value) ? value : 0;
    }

    private AtomicReference<Core> core = new AtomicReference<>(new Core(INITIAL_CAPACITY));


    /**
     * Returns value for the corresponding key or zero if this key is not present.
     *
     * @param key a positive key.
     * @return value for the corresponding or zero if this key is not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    public int get(int key) {
        if (key <= 0) throw new IllegalArgumentException("Key must be positive: " + key);
        int res = core.get().getInternal(key);
        return toValue(res);
    }

    /**
     * Changes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key   a positive key.
     * @param value a positive value.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key or value are not positive, or value is equal to
     *                                  {@link Integer#MAX_VALUE} which is reserved.
     */
    public int put(int key, int value) {
        if (key <= 0) throw new IllegalArgumentException("Key must be positive: " + key);
        if (!isValue(value)) throw new IllegalArgumentException("Invalid value: " + value);
        return toValue(putAndRehashWhileNeeded(key, value));
    }

    /**
     * Removes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key a positive key.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    public int remove(int key) {
        if (key <= 0) throw new IllegalArgumentException("Key must be positive: " + key);
        return toValue(putAndRehashWhileNeeded(key, DEL_VALUE));
    }

    private int putAndRehashWhileNeeded(int key, int value) {
        while (true) {
            int oldValue = core.get().putInternal(key, value);
            if (oldValue != NEEDS_REHASH)
                return oldValue;
            Core old = core.get();
            core.compareAndSet(old, core.get().rehash());
        }
    }

    private static class Core {
        final AtomicIntegerArray map; // pairs of key, value here
        final int shift;
        AtomicReference<Core> next;


        /**
         * Creates new core with a given capacity for (key, value) pair.
         * The actual size of the map is twice as big.
         */
        Core(int capacity) {
            map = new AtomicIntegerArray(2 * capacity);
            int mask = capacity - 1;
            assert mask > 0 && (mask & capacity) == 0 : "Capacity must be power of 2: " + capacity;
            shift = 32 - Integer.bitCount(mask);
            next = new AtomicReference<>(null);
        }

        int getInternal(int key) {
            int index = index(key), probes = 0;
            while (map.get(index) != key) { // optimize for successful lookup
                if (map.get(index) == NULL_KEY)
                    return NULL_VALUE; // not found -- no value
                if (++probes >= MAX_PROBES)
                    return NULL_VALUE;
                if (index == 0)
                    index = map.length();
                index -= 2;
            }
            int val = map.get(index + 1);
            if (val < 0) {
                help(index);
                return next.get().getInternal(key);
            }
            return val;
        }

        private void takeEmptySlot(int key, int val) {
            int index = index(key), probes = 0;
            while (map.get(index) != key) {
                if (map.get(index) == NULL_KEY && map.compareAndSet(index, NULL_KEY, key)) {
                    break;
                }
                if (map.get(index) == key) {
                    break;
                }
                if (++probes >= MAX_PROBES) {
                    return;
                }
                if (index == 0) {
                    index = map.length();
                }
                index -= 2;
            }
            map.compareAndSet(index + 1, NULL_VALUE, val);
        }


        private void help(int index) {
            int value = map.get(index + 1);
            assert value < 0;
            if (value != DONE_VALUE) {
                next.get().takeEmptySlot(map.get(index), value & Integer.MAX_VALUE);
            }
            map.set(index + 1, DONE_VALUE);
        }



        int putInternal(int key, int value) {
            assert value != 0;
            int index = index(key);
            int probes = 0;

            while (map.get(index) != key) { // optimize for successful lookup
                if (map.get(index) == NULL_KEY) {
                    // not found -- claim this slot
                    if (value == DEL_VALUE)
                        return NULL_VALUE; // remove of missing item, no need to claim slot
                    if (map.compareAndSet(index, NULL_KEY, key)) {
                        break;
                    }
                }
                if (map.get(index) == key) break;
                if (++probes >= MAX_PROBES)
                    return NEEDS_REHASH;
                if (index == 0)
                    index = map.length();
                index -= 2;
            }
            // found key -- update value
            int oldValue;

            do {
                oldValue = map.get(index + 1);
                if (oldValue < 0) {
                    help(index);
                    return next.get().putInternal(key, value);
                }
            } while (!map.compareAndSet(index + 1, oldValue, value));
            return oldValue;
        }


        Core rehash() {
            Core newCore = new Core(map.length()); // map.length is twice the current capacity
            next.compareAndSet(null, newCore);
            for (int index = 0; index < map.length(); index += 2) {
                int val = map.get(index + 1);
                if (val == DONE_VALUE) continue;
                while (true) {
                    if (val == DEL_VALUE) {
                        if (map.compareAndSet(index + 1, val, DONE_VALUE)) {
                            break;
                        }
                    }
                    if (val < 0 ) {
                        break;
                    }
                    if (val != DEL_VALUE && map.compareAndSet(index + 1, val, val | Integer.MIN_VALUE)) {
                        break;
                    }
                    val = map.get(index + 1);
                }
                help(index);
            }
            return next.get();
        }

        /**
         * Returns an initial index in map to look for a given key.
         */
        int index(int key) {
            return ((key * MAGIC) >>> shift) * 2;
        }

    }
}
