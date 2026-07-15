package com.orderiq.data.model;

import java.time.Instant;

/** Published after the database replacement commits; the semantic index will subscribe later. */
public record OrdersReloadedEvent(int orderCount, Instant occurredAt) {
}
