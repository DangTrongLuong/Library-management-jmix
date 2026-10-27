package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum UserStatus implements EnumClass<String> {

    ACTIVE("ACTIVE"),
    LOCKED("LOCKED"),
    INACTIVE("INACTIVE");

    private final String id;

    UserStatus(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static UserStatus fromId(String id) {
        for (UserStatus status : UserStatus.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        return null;
    }
}
