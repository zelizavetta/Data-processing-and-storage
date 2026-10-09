package keyserver;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class KeyClient {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String name = args[2];

        int delaySeconds = 0;
        boolean crash = false;
        for (int i = 3; i < args.length; i++) {
            switch (args[i]) {
                case "--delay" -> delaySeconds = Integer.parseInt(args[++i]);
                case "--crash" -> crash = true;
                default -> {
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    System.exit(1);
                }
            }
        }

        run(host, port, name, delaySeconds, crash);
    }

    private static void printUsage() {
        System.err.println("Usage: KeyClient <host> <port> <name> [--delay <seconds>] [--crash]");
        System.err.println("  --delay N  wait N seconds after sending the request before reading the response");
        System.err.println("  --crash    disconnect immediately after sending the request, without reading");
    }

    static void run(String host, int port, String name, int delaySeconds, boolean crash) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            OutputStream out = socket.getOutputStream();
            out.write(name.getBytes(StandardCharsets.US_ASCII));
            out.write(0);
            out.flush();

            if (crash) {
                System.out.println("Simulating client crash: closing connection without reading the response.");
                return;
            }

            if (delaySeconds > 0) {
                System.out.println("Simulating slow client: waiting " + delaySeconds + "s before reading the response...");
                try {
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            DataInputStream in = new DataInputStream(socket.getInputStream());
            byte[] certDer = Protocol.readFrame(in);
            byte[] keyDer = Protocol.readFrame(in);

            Path keyPath = Path.of(name + ".key");
            Path crtPath = Path.of(name + ".crt");
            PemUtil.writePem(keyDer, "PRIVATE KEY", keyPath);
            PemUtil.writePem(certDer, "CERTIFICATE", crtPath);

            System.out.println("Saved " + keyPath.toAbsolutePath() + " and " + crtPath.toAbsolutePath());
        }
    }
}
