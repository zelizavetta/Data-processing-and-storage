package keyserver;

import keyserver.KeyCertGenerator.KeyCertPair;
import keyserver.KeyRegistry.Lookup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public final class KeyServer {

    private static final int RSA_KEY_BITS = 8192;
    private static final int MAX_NAME_LEN = 4096;

    private final int port;
    private final ExecutorService keyGenPool;
    private final PrivateKey caKey;
    private final String issuerName;

    private final KeyRegistry registry = new KeyRegistry();
    private final BlockingQueue<Delivery> deliveryQueue = new LinkedBlockingQueue<>();

    private Selector selector;

    public KeyServer(int port, int genThreads, PrivateKey caKey, String issuerName) {
        this.port = port;
        this.keyGenPool = Executors.newFixedThreadPool(genThreads);
        this.caKey = caKey;
        this.issuerName = issuerName;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: KeyServer <port> <genThreads> <caKeyPem> <issuerDN>");
            System.err.println("  example issuerDN: \"CN=Task J1 CA,O=University\"");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        int genThreads = Integer.parseInt(args[1]);
        PrivateKey caKey = PemUtil.readPkcs8PrivateKey(Path.of(args[2]));
        String issuerName = args[3];

        new KeyServer(port, genThreads, caKey, issuerName).run();
    }

    public void run() throws IOException {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
             Selector sel = Selector.open()) {
            this.selector = sel;
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("KeyServer listening on port " + port
                    + " (RSA " + RSA_KEY_BITS + "-bit, " + issuerName + ")");

            while (true) {
                selector.select();
                drainDeliveries();

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (IOException e) {
                        closeQuietly(key);
                    }
                }
            }
        } finally {
            keyGenPool.shutdown();
        }
    }

    private void drainDeliveries() {
        Delivery d;
        while ((d = deliveryQueue.poll()) != null) {
            if (!d.key().isValid()) {
                continue;
            }
            if (d.pair() != null) {
                prepareWrite(d.key(), d.pair());
            } else {
                closeQuietly(d.key());
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel channel = server.accept();
        if (channel == null) {
            return;
        }
        channel.configureBlocking(false);
        SelectionKey clientKey = channel.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new ClientConnection());
    }

    private void handleRead(SelectionKey key) throws IOException {
        ClientConnection conn = (ClientConnection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buf = ByteBuffer.allocate(4096);
        int n;
        try {
            n = channel.read(buf);
        } catch (IOException e) {
            closeQuietly(key);
            return;
        }
        if (n == -1) {
            closeQuietly(key);
            return;
        }
        buf.flip();
        while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b == 0) {
                String name = conn.nameBytes.toString(StandardCharsets.US_ASCII);
                key.interestOps(0);
                onNameReceived(key, name);
                return;
            }
            conn.nameBytes.write(b);
            if (conn.nameBytes.size() > MAX_NAME_LEN) {
                closeQuietly(key);
                return;
            }
        }
    }

    private void onNameReceived(SelectionKey key, String name) {
        Lookup lookup = registry.lookupOrRegister(name, key);
        if (lookup.readyPair() != null) {
            prepareWrite(key, lookup.readyPair());
            return;
        }
        if (lookup.mustGenerate()) {
            keyGenPool.submit(() -> generateAndPublish(name));
        }
    }

    private void generateAndPublish(String name) {
        List<SelectionKey> waitingKeys;
        try {
            KeyCertPair pair = KeyCertGenerator.generate(name, caKey, issuerName, RSA_KEY_BITS);
            waitingKeys = registry.publishSuccess(name, pair);
            for (SelectionKey k : waitingKeys) {
                deliveryQueue.add(new Delivery(k, pair));
            }
        } catch (Exception e) {
            System.err.println("Key generation failed for '" + name + "': " + e);
            waitingKeys = registry.publishFailure(name);
            for (SelectionKey k : waitingKeys) {
                deliveryQueue.add(new Delivery(k, null));
            }
        }
        selector.wakeup();
    }

    private void prepareWrite(SelectionKey key, KeyCertPair pair) {
        ClientConnection conn = (ClientConnection) key.attachment();
        byte[] certFrame = Protocol.frame(pair.certificateDer());
        byte[] keyFrame = Protocol.frame(pair.privateKey().getEncoded());

        ByteBuffer buffer = ByteBuffer.allocate(certFrame.length + keyFrame.length);
        buffer.put(certFrame).put(keyFrame).flip();
        conn.writeBuffer = buffer;
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void handleWrite(SelectionKey key) throws IOException {
        ClientConnection conn = (ClientConnection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(conn.writeBuffer);
        if (!conn.writeBuffer.hasRemaining()) {
            closeQuietly(key);
        }
    }

    private void closeQuietly(SelectionKey key) {
        key.cancel();
        try {
            key.channel().close();
        } catch (IOException ignored) {
        }
    }

    private record Delivery(SelectionKey key, KeyCertPair pair) {}
}
