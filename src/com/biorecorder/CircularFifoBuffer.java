package com.biorecorder;

public class CircularFifoBuffer {

    private final double[] elements;
    private int start = 0;
    private int end = 0;
    private boolean full = false;
    private final int maxElements;
    /**
     * Constructs a new <code>BoundedFifoBuffer</code> big enough to hold
     * 32 elements.
     */
    public CircularFifoBuffer() {
        this(32);
    }
    /**
     * Constructs a new <code>BoundedFifoBuffer</code> big enough to hold
     * the specified number of elements.
     *
     * @param size  the maximum number of elements for this fifo
     * @throws IllegalArgumentException  if the size is less than 1
     */
    public CircularFifoBuffer(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        elements = new double[size];
        maxElements = elements.length;
    }
   
    /**
     * Returns the number of elements stored in the buffer.
     *
     * @return this buffer's size
     */
    public int size() {
        int size = 0;
        if (end < start) {
            size = maxElements - start + end;
        } else if (end == start) {
            size = (full ? maxElements : 0);
        } else {
            size = end - start;
        }
        return size;
    }
    /**
     * Returns true if this buffer is empty; false otherwise.
     *
     * @return true if this buffer is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    /**
     * Returns true if the array is full and no new elements can be added.
     *
     * @return <code>true</code> if the array is full
     */
    public boolean isFull() {
        return size() == maxElements;
    }

    /**
     * Gets the maximum size of the array (the bound).
     *
     * @return the maximum number of elements the array can hold
     */
    public int maxSize() {
        return maxElements;
    }

    /**
     * Clears this buffer.
     */
    public void clear() {
        full = false;
        start = 0;
        end = 0;
    }

    /**
     * Adds the given element to this buffer.
     */
    public boolean add(double element) {
        if (isFull()) {
            remove();
        }
        elements[end++] = element;
        if (end >= maxElements) {
            end = 0;
        }
        if (end == start) {
            full = true;
        }
        return true;
    }
    /**
     * Returns the least recently inserted element in this buffer.
     *
     * @return the least recently inserted element
     */
    public double get() {
        if (isEmpty()) {
            throw new IllegalStateException("The buffer is already empty");
        }
        return elements[start];
    }
    /**
     * Removes the least recently inserted element from this buffer.
     *
     */
    public void remove() {
        if (isEmpty()) {
            throw new IllegalStateException("The buffer is already empty");
        }
        start++;
        if (start >= maxElements) {
            start = 0;
        }
        full = false;
    }
    /**
     * Increments the internal index.
     *
     * @param index  the index to increment
     * @return the updated index
     */
    private int increment(int index) {
        index++;
        if (index >= maxElements) {
            index = 0;
        }
        return index;
    }
    /**
     * Decrements the internal index.
     *
     * @param index  the index to decrement
     * @return the updated index
     */
    private int decrement(int index) {
        index--;
        if (index < 0) {
            index = maxElements - 1;
        }
        return index;
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {
        int bufferSize = 3;
        CircularFifoBuffer buffer = new CircularFifoBuffer(bufferSize);
        boolean isTestOk = true;
        for (int i = 1; i < 10; i++) {
            buffer.add(i);
            double expected = (i <= bufferSize) ? 1 : i - bufferSize + 1;
            if(expected != buffer.get()) {
                System.out.println(i + "  Get from buffer: "+ buffer.get() + " Expected: "+ expected);
                isTestOk = false;
                break;
            }
        }
        System.out.println("Is test ok: "+isTestOk);
    }
}