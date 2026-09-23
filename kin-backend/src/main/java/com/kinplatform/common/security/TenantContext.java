package com.kinplatform.common.security;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> currentOrganizationId = new ThreadLocal<>();

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
    }

    public static UUID getOrDefault() {
        UUID id = currentOrganizationId.get();
        return id != null ? id : UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}