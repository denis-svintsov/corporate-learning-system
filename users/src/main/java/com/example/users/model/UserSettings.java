package com.example.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_settings", schema = "users")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "email_notifications")
    private Boolean emailNotifications;

    @Column(name = "push_notifications")
    private Boolean pushNotifications;

    @Column(name = "language")
    private String language;

    @Column(name = "timezone")
    private String timezone;
}
