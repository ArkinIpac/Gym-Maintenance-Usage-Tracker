package auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import structures.CustomLinkedList;

// Accounts in a text file, one per line: username|ROLE|salt|hash. Used while MySQL is offline.
// It never holds a password, and it's in .gitignore so accounts stay on the machine that made them.
public class FileUserRepository implements UserRepository {

    private final Path file;

    public FileUserRepository(Path file) {
        this.file = file;
    }

    @Override
    public CustomLinkedList<User> loadAll() {
        CustomLinkedList<User> out = new CustomLinkedList<>();
        if (!Files.exists(file)) return out;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.trim().split("\\|");
                if (p.length != 4) continue;
                out.add(new User(p[0], Role.parse(p[1]), p[2], p[3]));
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not read " + file + ": " + e.getMessage(), e);
        }
        return out;
    }

    // Written to a temp file first, then moved over the real one, so a crash mid-write
    // can't leave a half-written users file.
    @Override
    public void saveAll(CustomLinkedList<User> users) {
        StringBuilder sb = new StringBuilder();
        for (User u : users) {
            sb.append(u.getUsername()).append('|').append(u.getRole().name()).append('|')
              .append(u.getSalt()).append('|').append(u.getPasswordHash()).append('\n');
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save users: " + e.getMessage(), e);
        }
    }

    public boolean exists() { return Files.exists(file); }

    @Override
    public String describe() { return file.getFileName().toString(); }

}
