package listsort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        String type = args.length > 0 ? args[0] : "linked";
        int threadsCount = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        long delay = args.length > 2 ? Long.parseLong(args[2]) : 1000;

        StringList list = createList(type);
        Counter steps = new Counter();
        List<Thread> threads = startSorters(list, threadsCount, delay, steps);

        System.out.println("список: " + type + ", нитей: " + threadsCount + ", задержка: " + delay + " мс");
        System.out.println("пустая строка - вывести список, Ctrl+Z/Ctrl+D - выход");

        long start = System.currentTimeMillis();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) {
                list.print(System.out);
                continue;
            }
            for (int i = 0; i < line.length(); i += 80) {
                list.addFirst(line.substring(i, Math.min(i + 80, line.length())));
            }
        }

        stopSorters(threads);
        list.print(System.out);
        System.out.println("всего шагов: " + steps.get() + " за "
                + (System.currentTimeMillis() - start) / 1000 + " с");
    }

    static StringList createList(String type) {
        if (type.equals("linked")) return new MyLinkedList();
        if (type.equals("array")) return new SyncList();
        throw new IllegalArgumentException("неизвестный тип списка: " + type);
    }

    static List<Thread> startSorters(StringList list, int count, long delay, Counter steps) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        list.sortPass(delay, steps);
                    }
                } catch (InterruptedException e) {
                }
            });
            t.start();
            threads.add(t);
        }
        return threads;
    }

    static void stopSorters(List<Thread> threads) throws InterruptedException {
        for (Thread t : threads) t.interrupt();
        for (Thread t : threads) t.join();
    }
}
