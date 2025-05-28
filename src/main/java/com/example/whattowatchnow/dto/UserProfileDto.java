package com.example.whattowatchnow.dto;

public class UserProfileDto {
    private String id;
    private String nickname;
    private String email;
    private String profilePicture;

    public UserProfileDto() {
    }

    public UserProfileDto(String id, String nickname, String email, String profilePicture) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
        this.profilePicture = profilePicture;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
