package io.sumith;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Main {

    private static final Path downloadedFolderLocation = Path.of("/home/sumith/Downloads/instagram/connections");

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        Main mainInstance = new Main();

        if (args.length == 0) {
            ObjectMapper objectMapper = new ObjectMapper();

            // Read followers from file
            Set<InstagramUser> followersUsernames = objectMapper.readValue(
                new File("followers.json"),
                objectMapper.getTypeFactory().constructCollectionType(Set.class, InstagramUser.class)
            );

            // Read following from file
            Set<InstagramUser> followingUsers = objectMapper.readValue(
                new File("following.json"),
                objectMapper.getTypeFactory().constructCollectionType(Set.class, InstagramUser.class)
            );

            // Convert followers to a set of usernames
            Set<String> followersSet = new HashSet<>();
            followersUsernames.forEach(user -> followersSet.add(user.getUsername()));

            // Find users not following back
            String[] notFollowingBack = followingUsers.stream()
                .map(InstagramUser::getUsername)
                .filter(username -> !followersSet.contains(username))
                .toArray(String[]::new);

            System.out.println("Followers: " + followersSet);

            for (int i = 0; i < notFollowingBack.length; i++) {
                System.out.println((i + 1) + ". " + notFollowingBack[i]);
            }

        } else if (args[0].equals("get")) {
            System.out.print("Enter username: ");
            String username = scanner.next();

            System.out.print("Enter userId: ");
            String userId = scanner.next();

            System.out.println("Choose the type:");
            System.out.println("1. Following list");
            System.out.println("2. Followers list");

            System.out.print("Enter type: ");
            int choice = scanner.nextInt();

            if (choice == 1 || choice == 2) {
                mainInstance.getListFromInstagram(username, userId, choice == 1 ? "following" : "followers");
            } else {
                System.out.println("Invalid choice. Please select a valid option.");
            }
        } else if ("not".equals(args[0])) {
            mainInstance.handleNotFollowing(scanner, true);
        } else if ("back".equals(args[0])) {
            mainInstance.handleNotFollowing(scanner, false);
        } else {
            System.out.println("Invalid argument. Please use a valid option.");
        }
    }

    private void handleNotFollowing(Scanner scanner, boolean notFollowingBack) throws IOException {
        Set<InstagramUser> followersUsernames = getListFromFile("followers");
        Set<InstagramUser> followingUsers = getListFromFile("following");

        Set<String> comparisonSet = new HashSet<>();
        Set<String> targetSet = new HashSet<>();

        if (notFollowingBack) {
            followersUsernames.forEach(user -> comparisonSet.add(user.getUsername()));
            followingUsers.forEach(user -> targetSet.add(user.getUsername()));
        } else {
            followingUsers.forEach(user -> comparisonSet.add(user.getUsername()));
            followersUsernames.forEach(user -> targetSet.add(user.getUsername()));
        }

        String[] results = targetSet.stream()
            .filter(username -> !comparisonSet.contains(username))
            .toArray(String[]::new);

        for (int i = 0; i < results.length; i++) {
            System.out.println((i + 1) + ". " + results[i]);
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.browse(new URI("https://www.instagram.com/" + results[i]));
                } catch (URISyntaxException e) {
                    System.err.println("Invalid URI for user: " + results[i]);
                } catch (IOException e) {
                    System.err.println("Error opening browser: " + e.getMessage());
                }
            } else {
                System.out.println("Desktop is not supported on this system.");
            }
        }
    }

    public HttpRequest buildRequest(String url, String username, String type) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

            JsonNode rootNode = objectMapper.readTree(new File("header.json"));
            rootNode.fieldNames().forEachRemaining(fieldName -> {
                String value = rootNode.get(fieldName).asText();
                if ("Referer".equals(fieldName)) {
                    requestBuilder.header(fieldName, "https://www.instagram.com/" + username + "/" + type + "/");
                } else {
                    requestBuilder.header(fieldName, value);
                }
            });

            return requestBuilder.build();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return null;
        }
    }

    private void getListFromInstagram(String username, String userId, String type) throws Exception {
        Set<InstagramUser> instagramUsers = new HashSet<>();
        boolean hasMore = true;

        for (int i = 0; hasMore; i++) {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                String url = "https://www.instagram.com/api/v1/friendships/" + userId + "/" + type + "/?count=12"
                        + (i == 0 ? "" : "&max_id=" + (i * 12))
                        + (!"followers".equals(type) ? "&search_surface=follow_list_page" : "");

                HttpRequest request = buildRequest(url, username, type);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.body());

                JsonNode usersNode = rootNode.path("users");
                for (JsonNode userNode : usersNode) {
                    InstagramUser user = new InstagramUser();
                    user.setFullName(userNode.path("full_name").asText());
                    user.setUsername(userNode.path("username").asText());
                    instagramUsers.add(user);
                }

                System.out.printf("Fetched %d users on iteration %d%n", usersNode.size(), i + 1);
                hasMore = !usersNode.isEmpty();
            } catch (Exception e) {
                System.err.println("Error during iteration " + i + ": " + e.getMessage());
            }
        }

        System.out.println("Total users fetched: " + instagramUsers.size());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new File(type + ".json"), instagramUsers);
        System.out.println("List saved to " + type + ".json");
    }

    private Set<InstagramUser> getListFromFile(String type) {
        Set<InstagramUser> instagramUsers = new HashSet<>();
        String fileName = Objects.equals(type, "following") ? "followers_and_following/following.json"
                : "followers_and_following/followers_1.json";
        Path filePath = downloadedFolderLocation.resolve(fileName);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(filePath.toFile());
            JsonNode targetNode = "following".equals(type) ? rootNode.path("relationships_following") : rootNode;

            for (JsonNode userNode : targetNode) {
                InstagramUser user = new InstagramUser();
                user.setUsername(userNode.path("string_list_data").get(0).path("value").asText());
                user.setFullName("");
                instagramUsers.add(user);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return instagramUsers;
    }
}
