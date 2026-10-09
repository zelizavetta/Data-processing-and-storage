package listsort;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class StressTest {

    public static void main(String[] args) throws InterruptedException {
        test("linked");
        test("array");
    }

    static void test(String type) throws InterruptedException {
        StringList list = Main.createList(type);
        Counter steps = new Counter();
        List<Thread> threads = Main.startSorters(list, 16, 1, steps);

        List<String> added = new ArrayList<>();
        Random rnd = new Random(7);
        int checks = 0;
        for (int i = 0; i < 150; i++) {
            String s = StepsTest.randomWord(rnd);
            list.addFirst(s);
            added.add(s);
            if (i % 4 == 0) {
                check(type, read(list), added);
                checks++;
            }
        }

        long deadline = System.currentTimeMillis() + 60_000;
        List<String> current = read(list);
        while (!isSorted(current)) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError(type + ": не отсортировалось за минуту");
            }
            check(type, current, added);
            checks++;
            Thread.sleep(50);
            current = read(list);
        }
        check(type, current, added);
        Main.stopSorters(threads);
        System.out.println(type + ": ok, проверок печати: " + checks + ", шагов: " + steps.get());
    }

    static List<String> read(StringList list) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        list.print(new PrintStream(buf));
        List<String> result = new ArrayList<>();
        for (String line : buf.toString().split("\n")) {
            if (!line.startsWith("---")) {
                result.add(line.substring(line.indexOf(": ") + 2));
            }
        }
        return result;
    }

    static void check(String type, List<String> printed, List<String> added) {
        List<String> a = new ArrayList<>(printed);
        List<String> b = new ArrayList<>(added);
        Collections.sort(a);
        Collections.sort(b);
        if (!a.equals(b)) {
            throw new AssertionError(type + ": напечатано " + printed.size() + " строк, добавлено " + added.size());
        }
    }

    static boolean isSorted(List<String> l) {
        for (int i = 1; i < l.size(); i++) {
            if (l.get(i - 1).compareTo(l.get(i)) > 0) return false;
        }
        return true;
    }
}
