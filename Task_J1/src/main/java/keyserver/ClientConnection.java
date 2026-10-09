package keyserver;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

final class ClientConnection {
    final ByteArrayOutputStream nameBytes = new ByteArrayOutputStream(64);
    ByteBuffer writeBuffer;
}
