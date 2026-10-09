package keyserver;

import keyserver.KeyCertGenerator.KeyCertPair;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class KeyRegistry {

    private final Map<String, KeyCertPair> ready = new HashMap<>();
    private final Set<String> pending = new HashSet<>();
    private final Map<String, List<SelectionKey>> waiters = new HashMap<>();

    record Lookup(KeyCertPair readyPair, boolean mustGenerate) {}

    synchronized Lookup lookupOrRegister(String name, SelectionKey key) {
        KeyCertPair cached = ready.get(name);
        if (cached != null) {
            return new Lookup(cached, false);
        }
        waiters.computeIfAbsent(name, n -> new ArrayList<>()).add(key);
        if (pending.contains(name)) {
            return new Lookup(null, false);
        }
        pending.add(name);
        return new Lookup(null, true);
    }

    synchronized List<SelectionKey> publishSuccess(String name, KeyCertPair pair) {
        ready.put(name, pair);
        pending.remove(name);
        List<SelectionKey> keys = waiters.remove(name);
        return keys == null ? List.of() : keys;
    }

    synchronized List<SelectionKey> publishFailure(String name) {
        pending.remove(name);
        List<SelectionKey> keys = waiters.remove(name);
        return keys == null ? List.of() : keys;
    }
}
