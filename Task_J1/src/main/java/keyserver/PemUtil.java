package keyserver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public final class PemUtil {

    private PemUtil() {}

    public static void writePem(byte[] der, String type, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        String b64 = Base64.getEncoder().encodeToString(der);
        for (int i = 0; i < b64.length(); i += 64) {
            sb.append(b64, i, Math.min(i + 64, b64.length())).append('\n');
        }
        sb.append("-----END ").append(type).append("-----\n");
        Files.writeString(path, sb.toString(), StandardCharsets.US_ASCII);
    }

    public static void writePrivateKeyPem(PrivateKey key, Path path) throws IOException {
        writePem(key.getEncoded(), "PRIVATE KEY", path);
    }

    public static void writeCertificatePem(byte[] certDer, Path path) throws IOException {
        writePem(certDer, "CERTIFICATE", path);
    }

    public static byte[] decodePem(String pem, String type) {
        String header = "-----BEGIN " + type + "-----";
        String footer = "-----END " + type + "-----";
        int start = pem.indexOf(header);
        int end = pem.indexOf(footer);
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException("PEM block of type '" + type + "' not found");
        }
        start += header.length();
        String b64 = pem.substring(start, end).replaceAll("\\s", "");
        return Base64.getDecoder().decode(b64);
    }

    public static PrivateKey readPkcs8PrivateKey(Path path) throws Exception {
        String pem = Files.readString(path, StandardCharsets.US_ASCII);
        byte[] der = decodePem(pem, "PRIVATE KEY");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
