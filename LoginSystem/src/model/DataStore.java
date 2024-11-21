/**
 * The {@code DataStore} class is a singleton designed to manage the storage of user data
 * and invitation-related information. It ensures a centralized and single-point
 * management of these entities throughout the application.
 *
 * <p>This class contains:
 * <ul>
 *   <li>A list to store {@code User} objects.</li>
 *   <li>A map to associate invitation codes with their respective {@code User.Invitation} objects.</li>
 * </ul>
 *
 * <p>The class employs a private constructor to prevent instantiation
 * and provides a thread-unsafe singleton instance through the {@code getInstance()} method.
 */
package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataStore {

    // Singleton instance to ensure a single instance of DataStore throughout the application
    private static DataStore instance = null;

    /**
     * A list to store {@code User} objects representing registered users.
     */
    private List<User> userList = new ArrayList<>();

    /**
     * A map associating invitation codes (as {@code String}) with their corresponding
     * {@code User.Invitation} details.
     */
    private Map<String, User.Invitation> invitations = new HashMap<>();

    /**
     * Private constructor to prevent direct instantiation of the {@code DataStore} class.
     * Use {@code getInstance()} to obtain the singleton instance.
     */
    private DataStore() {}

    /**
     * Retrieves the singleton instance of {@code DataStore}.
     * If the instance does not already exist, it initializes it.
     *
     * @return The singleton {@code DataStore} instance.
     */
    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    /**
     * Returns the list of registered users.
     *
     * @return A {@code List} of {@code User} objects.
     */
    public List<User> getUserList() {
        return userList;
    }

    /**
     * Returns the map of invitation codes and their associated invitation details.
     *
     * @return A {@code Map} where the key is the invitation code ({@code String}),
     *         and the value is the associated {@code User.Invitation} object.
     */
    public Map<String, User.Invitation> getInvitations() {
        return invitations;
    }

    /**
     * Finds a user by their username.
     *
     * <p>This method searches through the {@code userList} and returns the {@code User}
     * object with a matching username. If no match is found, it returns {@code null}.
     *
     * @param username The username to search for.
     * @return The {@code User} object with the matching username, or {@code null} if not found.
     */
    public User findUserByUsername(String username) {
        return userList.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}
