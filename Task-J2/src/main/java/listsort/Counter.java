package listsort;

class Counter {
    private long value;

    synchronized void inc() {
        value++;
    }

    synchronized long get() {
        return value;
    }
}
