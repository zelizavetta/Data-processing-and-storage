package listsort;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SyncList implements StringList {

    private final List<String> list = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addFirst(String s) {
        list.add(0, s);
    }

    @Override
    public void print(PrintStream out) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        synchronized (list) {
            for (String s : list) {
                n++;
                sb.append(n).append(": ").append(s).append('\n');
            }
        }
        out.print("--- " + n + " строк ---\n" + sb);
    }

    @Override
    public void sortPass(long delay, Counter steps) throws InterruptedException {
        for (int i = 0; ; i++) {
            Thread.sleep(delay);
            synchronized (list) {
                if (i + 1 >= list.size()) return;
                Thread.sleep(delay);
                String a = list.get(i);
                String b = list.get(i + 1);
                if (a.compareTo(b) > 0) {
                    list.set(i, b);
                    list.set(i + 1, a);
                }
                steps.inc();
            }
        }
    }
}
