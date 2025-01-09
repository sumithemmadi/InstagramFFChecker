package io.sumith;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class InstagramInfo {

   private Set<InstagramUser> followers = new HashSet<>();
   private Set<InstagramUser> following = new HashSet<>();

   public Integer getFollowersCount() {
      return followers.size();
   }

   public Integer getFollowingCount() {
      return following.size();
   }

   public Set<InstagramUser> getFollowers() {
      return followers;
   }

   public void setFollowers(Set<InstagramUser> followers) {
      this.followers = followers;
   }

   public Set<InstagramUser> getFollowing() {
      return following;
   }

   public void setFollowing(Set<InstagramUser> following) {
      this.following = following;
   }

   @Override
   public String toString() {
      return "InstagramInfo{" +
              "followers=" + followers +
              ", following=" + following +
              '}';
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      InstagramInfo that = (InstagramInfo) o;
      return Objects.equals(followers, that.followers) && Objects.equals(following, that.following);
   }

   @Override
   public int hashCode() {
      return Objects.hash(followers, following);
   }
}
