package com.smarturl.entity;

/**
 * User roles for authorization.
 * USER  — can create/manage own URLs and view own analytics.
 * ADMIN — can view all users, all URLs, platform stats, and audit logs.
 */
public enum Role {
    USER,
    ADMIN
}