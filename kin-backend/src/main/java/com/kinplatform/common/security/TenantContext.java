package com.kinplatform.common.security;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> currentOrganizationId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID organizationId) {
        currentOrganizationId.set(organizationId);
    }

    public static UUID get() {
        return currentOrganizationId.get();
    }

    public static boolean isSet() {
        return currentOrganizationId.get() != null;
    }

    public static void clear() {
        currentOrganizationId.remove();
        currentUserId.remove();
    }

    public static UUID getOrDefault() {
        UUID id = currentOrganizationId.get();
        return id != null ? id : UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    // UserId support for tenant filtering (Platform/Enterprise multi-tenancy)
    public static void setCurrentUserId(UUID userId) {
        currentUserId.set(userId);
    }

    public static UUID getCurrentUserId() {
        return currentUserId.get();
    }

    public static boolean isCurrentUserIdSet() {
        return currentUserId.get() != null;
    }

    public static void clearCurrentUserId() {
        currentUserId.remove();
    }
}