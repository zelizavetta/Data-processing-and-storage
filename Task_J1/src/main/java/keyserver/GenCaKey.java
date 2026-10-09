package keyserver;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public final class GenCaKey {

    private GenCaKey() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: GenCaKey <output-key.pem> [bits=4096]");
            System.exit(1);
        }
        int bits = args.length > 1 ? Integer.parseInt(args[1]) : 4096;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(bits);
        KeyPair keyPair = kpg.generateKeyPair();

        Path out = Path.of(args[0]);
        PemUtil.writePrivateKeyPem(keyPair.getPrivate(), out);
        System.out.println("CA signing key (" + bits + " bit) written to " + out.toAbsolutePath());
    }
}
