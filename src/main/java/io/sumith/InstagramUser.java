package io.sumith;

import java.util.Objects;

public class InstagramUser {

    private String fullName;
    private String username;

    public InstagramUser(){

    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "InstagramUser{" +
                "fullName='" + fullName + '\'' +
                ", username='" + username + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InstagramUser that = (InstagramUser) o;
        return Objects.equals(fullName, that.fullName) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, username);
    }
}
