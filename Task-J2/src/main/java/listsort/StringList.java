package listsort;

import java.io.PrintStream;

interface StringList {

    void addFirst(String s);

    void print(PrintStream out);

    void sortPass(long delay, Counter steps) throws InterruptedException;
}
