package auth;

import structures.CustomHashTable;
import structures.CustomLinkedList;

// Slows down password guessing from one address. The per-account lockout in UserStore stops
// someone hammering one username; this stops one address trying many usernames.
// After 20 wrong sign-ins in 10 minutes, that address has to wait out the rest of the window.
public class LoginThrottle {

    private static final int MAX_FAILURES = 20;
    private static final long WINDOW_MILLIS = 10 * 60 * 1000;

    private static class Window {
        long start;
        int failures;
    }

    private final CustomHashTable<String, Window> byAddress = new CustomHashTable<>();
    private final CustomLinkedList<String> addresses = new CustomLinkedList<>();   // for clean-up

    public boolean isBlocked(String address) {
        Window w = current(address);
        return w != null && w.failures >= MAX_FAILURES;
    }

    public void recordFailure(String address) {
        Window w = current(address);
        if (w == null) {
            if (addresses.size() > 5000) removeExpired();
            w = new Window();
            w.start = System.currentTimeMillis();
            byAddress.put(address, w);
            addresses.add(address);
        }
        w.failures++;
    }

    public void recordSuccess(String address) {
        if (byAddress.remove(address)) addresses.remove(address);
    }

    // The window for this address, or null if it has none or it ran out.
    private Window current(String address) {
        Window w = byAddress.get(address);
        if (w != null && System.currentTimeMillis() - w.start > WINDOW_MILLIS) {
            recordSuccess(address);
            return null;
        }
        return w;
    }

    private void removeExpired() {
        CustomLinkedList<String> all = new CustomLinkedList<>();
        for (String a : addresses) all.add(a);
        for (String a : all) current(a);
    }
}
