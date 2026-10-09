package listsort;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

class MyLinkedList implements StringList, Iterable<String> {

    private static class Node {
        String value;
        Node next;
        ReentrantLock lock = new ReentrantLock();

        Node(String value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    private final Node head = new Node(null, null);

    @Override
    public void addFirst(String s) {
        head.lock.lock();
        try {
            head.next = new Node(s, head.next);
        } finally {
            head.lock.unlock();
        }
    }

    @Override
    public void print(PrintStream out) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String s : this) {
            n++;
            sb.append(n).append(": ").append(s).append('\n');
        }
        out.print("--- " + n + " строк ---\n" + sb);
    }

    @Override
    public void sortPass(long delay, Counter steps) throws InterruptedException {
        Node prev = head;
        while (true) {
            Thread.sleep(delay);

            Node p = prev;
            p.lock.lock();
            try {
                Node a = p.next;
                if (a == null) return;
                a.lock.lock();
                try {
                    Node b = a.next;
                    if (b == null) return;
                    b.lock.lock();
                    try {
                        Thread.sleep(delay);
                        if (a.value.compareTo(b.value) > 0) {
                            p.next = b;
                            a.next = b.next;
                            b.next = a;
                            prev = b;
                        } else {
                            prev = a;
                        }
                        steps.inc();
                    } finally {
                        b.lock.unlock();
                    }
                } finally {
                    a.lock.unlock();
                }
            } finally {
                p.lock.unlock();
            }
        }
    }

    @Override
    public Iterator<String> iterator() {
        head.lock.lock();
        return new Iterator<>() {
            Node cur = head;

            @Override
            public boolean hasNext() {
                if (cur == null) return false;
                if (cur.next != null) return true;
                cur.lock.unlock();
                cur = null;
                return false;
            }

            @Override
            public String next() {
                if (!hasNext()) throw new NoSuchElementException();
                Node nxt = cur.next;
                nxt.lock.lock();
                cur.lock.unlock();
                cur = nxt;
                return cur.value;
            }
        };
    }
}
