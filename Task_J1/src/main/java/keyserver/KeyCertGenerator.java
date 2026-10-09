package keyserver;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public final class KeyCertGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final AtomicLong SERIAL_SEQ = new AtomicLong(System.currentTimeMillis());
    private static final long VALIDITY_DAYS = 3650;

    private KeyCertGenerator() {}

    public record KeyCertPair(PrivateKey privateKey, byte[] certificateDer) {}

    public static KeyCertPair generate(String subjectName, PrivateKey caKey, String issuerName, int keyBits)
            throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(keyBits);
        KeyPair keyPair = kpg.generateKeyPair();

        X500Name issuer = new X500Name(issuerName);
        X500Name subject = new X500Name("CN=" + escapeDnValue(subjectName));
        BigInteger serial = BigInteger.valueOf(SERIAL_SEQ.incrementAndGet());
        Date notBefore = new Date();
        Date notAfter = Date.from(Instant.now().plus(VALIDITY_DAYS, ChronoUnit.DAYS));
        SubjectPublicKeyInfo pubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, pubKeyInfo);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(caKey);

        X509CertificateHolder holder = certBuilder.build(signer);
        return new KeyCertPair(keyPair.getPrivate(), holder.getEncoded());
    }

    private static String escapeDnValue(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\', ',', '=', '+', '<', '>', ';', '"' -> sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
