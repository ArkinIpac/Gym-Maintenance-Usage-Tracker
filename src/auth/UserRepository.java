package auth;

import structures.CustomLinkedList;

// Where accounts are kept between runs: the app_users table when MySQL is up,
// otherwise users.txt. UserStore works the same with either.
public interface UserRepository {

    // Every saved account, in the order they were added.
    CustomLinkedList<User> loadAll();

    // Replaces what's saved with this list, all or nothing.
    void saveAll(CustomLinkedList<User> users);

    // Shown on the Users page, e.g. "MySQL" or "users.txt".
    String describe();
}
