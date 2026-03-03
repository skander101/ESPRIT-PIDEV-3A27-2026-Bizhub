package com.bizhub.model.community.post;

import java.time.LocalDateTime;

public class Post {
    private int postId;
    private int userId;
    private String title;
    private String content;
    private String category;
    private LocalDateTime createdAt;
    private String authorName; // joined from user table, not a DB column
    private String mediaUrl;   // path to image or video file
    private String mediaType;  // "image" or "video"

    public Post() {}

    public Post(int userId, String title, String content, String category) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    @Override
    public String toString() {
        return "Post{postId=" + postId + ", title='" + title + "', category='" + category + "'}";
    }
}