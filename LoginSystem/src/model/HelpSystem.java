package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A system for managing help messages submitted by students.
 * Provides functionality to handle both generic messages and messages specific to queries/topics.
 */
public class HelpSystem {

    // Stores generic messages from students
    private static List<String> genericMessages = new ArrayList<>();

    // Stores specific messages from students with their queries
    private static Map<String, List<String>> specificMessages = new HashMap<>();

    /**
     * Adds a generic help message.
     *
     * @param message The generic message to be added.
     */
    public static void sendGenericMessage(String message) {
        genericMessages.add(message);
    }

    /**
     * Adds a specific help message with the student's query.
     *
     * @param query   The query/topic the student needs help with.
     * @param message The specific help message.
     */
    public static void sendSpecificMessage(String query, String message) {
        specificMessages.computeIfAbsent(query, k -> new ArrayList<>()).add(message);
    }

    /**
     * Retrieves all generic help messages.
     *
     * @return A list of generic messages submitted by students.
     */
    public static List<String> getGenericMessages() {
        return new ArrayList<>(genericMessages);
    }

    /**
     * Retrieves all specific help messages for a given query.
     *
     * @param query The query/topic to retrieve specific messages for.
     * @return A list of specific messages associated with the given query.
     *         Returns an empty list if no messages are associated with the query.
     */
    public static List<String> getSpecificMessages(String query) {
        return specificMessages.getOrDefault(query, new ArrayList<>());
    }

    /**
     * Retrieves a list of all queries/topics for which specific messages have been submitted.
     *
     * @return A list of query strings representing topics with specific messages.
     */
    public static List<String> getSpecificQueries() {
        return new ArrayList<>(specificMessages.keySet());
    }

    /**
     * Clears all stored help messages (both generic and specific).
     * This is useful for testing or resetting the system.
     */
    public static void clearHelpMessages() {
        genericMessages.clear();
        specificMessages.clear();
    }
}

