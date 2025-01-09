package io.sumith;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
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
        if(args.length == 0){
            ObjectMapper objectMapper = new ObjectMapper();
            Set<InstagramUser> followersUsernames = objectMapper.readValue(new File("followers.json"),
                    objectMapper.getTypeFactory().constructCollectionType(Set.class, InstagramUser.class));

            Set<InstagramUser> followingUsers = objectMapper.readValue(new File("following.json"),
                    objectMapper.getTypeFactory().constructCollectionType(Set.class, InstagramUser.class));

            Set<String> followersSet = Set.of(followersUsernames.stream().map(InstagramUser::getUsername).toArray(String[]::new));;

            String[] notFollowingBack = followingUsers.stream()
                    .map(InstagramUser::getUsername)
                    .filter(username -> !followersSet.contains(username))
                    .toArray(String[]::new);

            System.out.println(followersSet);

            for (int i = 0; i < notFollowingBack.length; i++) {
                System.out.println((i + 1) + ". " + notFollowingBack[i]);
            }

        } else if(args[0].equals("get")) {
            System.out.print("Enter username : ");
            String username = scanner.next();

            System.out.print("Enter userId : ");
            String userId = scanner.next();

            System.out.println("Choose the type:");
            System.out.println("1. Following list");
            System.out.println("2. Followers list");

            System.out.print("Enter type : ");
            int choice = scanner.nextInt();

            if(choice == 1 || choice == 2) {
                mainInstance.getListFromInstagram(username, userId, choice == 1 ? "following" : "followers");
            } else {
                System.out.println("Invalid choice. Please select a valid option.");
            }

        } else {
            Set<InstagramUser> followersUsernames = mainInstance.getListFromFile("followers");
            Set<InstagramUser> followingUsers = mainInstance.getListFromFile("following");

            Set<String> followersSet = Set.of(followersUsernames.stream().map(InstagramUser::getUsername).toArray(String[]::new));;

            String[] notFollowingBack = followingUsers.stream()
                    .map(InstagramUser::getUsername)
                    .filter(username -> !followersSet.contains(username))
                    .toArray(String[]::new);

            for (int i = 0; i < notFollowingBack.length; i++) {
                scanner.nextLine();
                System.out.println((i + 1) + ". " + notFollowingBack[i]);
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.browse(new URI("https://www.instagram.com/" + notFollowingBack[i]));
                } else {
                    System.out.println("Desktop is not supported on this system.");
                }

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
                JsonNode valueNode = rootNode.get(fieldName);
                String value = valueNode.asText();
                if("Referer".equals(fieldName)){
                    requestBuilder.header(fieldName,"https://www.instagram.com/"+username+"/"+type+"/");
                } else {
                    requestBuilder.header(fieldName, value);
                }
            });

            return requestBuilder.build();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }

        return null;
    }

    private void getListFromInstagram(String username, String userId, String type) throws Exception {
        Set<InstagramUser> instagramUsers = new HashSet<>();
        boolean runExistLoop = true;

        for (int i = 0; runExistLoop; i++) {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();

                String url = "https://www.instagram.com/api/v1/friendships/" + userId + "/" + type + "/?count=12"
                        + (i == 0 ? "" : "&max_id=" + (i * 12))
                        + (!Objects.equals(type, "followers") ? "&search_surface=follow_list_page" : "");

                HttpRequest request =  buildRequest(url, username, type);

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.body());

                JsonNode usersNode = rootNode.path("users");

                for (int k = 0; k < usersNode.size(); k++) {
                    InstagramUser instagramUser = new InstagramUser();
                    instagramUser.setFullName(usersNode.get(k).path("full_name").asText());
                    instagramUser.setUsername(usersNode.get(k).path("username").asText());
                    instagramUsers.add(instagramUser);
                }

                System.out.printf("%d -- %s\n", i + 1, usersNode.size());
                if (usersNode.isEmpty()) {
                    runExistLoop = false;
                }
            } catch (Exception e) {
                System.out.println("Error at iteration " + i);
            }
        }

        System.out.println(instagramUsers.size());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            objectMapper.writeValue(new File("following.json"), instagramUsers);
            System.out.println("List saved to following.json");
        } catch (IOException e) {
            System.out.println("Failed to save the list to JSON file: " + e.getMessage());
        }
    }

    private  Set<InstagramUser> getListFromFile(String type){
        Set<InstagramUser> instagramUsers = new HashSet<>();

        String fileName = Objects.equals(type, "following") ? "followers_and_following/following.json" : "followers_and_following/followers_1.json";
        Path filePath = downloadedFolderLocation.resolve(fileName);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode  = objectMapper.readTree(filePath.toFile());

            if(Objects.equals(type, "following")){
                JsonNode relationships_following = rootNode.path("relationships_following");

                for (int k = 0; k < relationships_following.size(); k++) {
                    InstagramUser instagramUser = new InstagramUser();
                    instagramUser.setUsername(relationships_following.get(k).path("string_list_data").get(0).path("value").asText());
                    instagramUser.setFullName("");
                    instagramUsers.add(instagramUser);
                }
            } else {
                for (int k = 0; k < rootNode.size(); k++) {
                    InstagramUser instagramUser = new InstagramUser();
                    instagramUser.setUsername(rootNode.get(k).path("string_list_data").get(0).path("value").asText());
                    instagramUser.setFullName("");
                    instagramUsers.add(instagramUser);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return instagramUsers;
    }
}
