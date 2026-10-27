package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum Gender implements EnumClass<String> {

    MALE("MALE", "Nam"),
    FEMALE("FEMALE", "Nữ"),
    OTHER("OTHER", "Khác");

    private final String id;
    private final String caption;

    Gender(String id, String caption) {
        this.id = id;
        this.caption = caption;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getCaption() {
        return caption;
    }

    @Nullable
    public static Gender fromId(String id) {
        for (Gender g : Gender.values()) {
            if (g.getId().equals(id)) {
                return g;
            }
        }
        return null;
    }
}
