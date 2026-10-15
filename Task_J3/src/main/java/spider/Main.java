package spider;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        Spider spider = new Spider(port);
        spider.run().forEach(System.out::println);
    }
}
