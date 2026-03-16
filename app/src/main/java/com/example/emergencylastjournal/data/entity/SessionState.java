package com.example.emergencylastjournal.data.entity;

/**
 * Defines the possible states of a safety session.
 */
public enum SessionState {
    /** No active tracking session. */
    IDLE,
    /** Tracking is active, GPS is logging, timer is counting down (> 5 mins). */
    ACTIVE,
    /** Timer is low (<= 5 mins), user is warned. */
    WARNING,
    /** Timer is critical (<= 1 min), automatic alerts may be sent. */
    URGENT,
    /** Timer has reached zero, emergency protocols active. */
    EMERGENCY,
    /** Session has ended. */
    TERMINATED
}