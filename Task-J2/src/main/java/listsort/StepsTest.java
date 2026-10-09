package listsort;

import java.util.List;
import java.util.Random;

public class StepsTest {

    public static void main(String[] args) throws InterruptedException {
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        long delay = args.length > 2 ? Long.parseLong(args[2]) : 100;

        System.out.println("строк: " + size + ", время: " + seconds + " с, задержка: " + delay + " мс");
        System.out.println("тип     нитей  теория   факт");
        for (String type : List.of("linked", "array")) {
            for (int k : new int[]{1, 2, 4, 8}) {
                long oneThread = seconds * 1000L / (2 * delay);
                long theory = type.equals("linked")
                        ? k * oneThread
                        : Math.min(k * oneThread, seconds * 1000L / delay);
                long real = run(type, size, seconds, delay, k);
                System.out.printf("%-7s %5d %7d %6d%n", type, k, theory, real);
            }
        }
    }

    static long run(String type, int size, int seconds, long delay, int k) throws InterruptedException {
        StringList list = Main.createList(type);
        Random rnd = new Random(42);
        for (int i = 0; i < size; i++) {
            list.addFirst(randomWord(rnd));
        }
        Counter steps = new Counter();
        List<Thread> threads = Main.startSorters(list, k, delay, steps);
        Thread.sleep(seconds * 1000L);
        Main.stopSorters(threads);
        return steps.get();
    }

    static String randomWord(Random rnd) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append((char) ('a' + rnd.nextInt(26)));
        }
        return sb.toString();
    }
}
