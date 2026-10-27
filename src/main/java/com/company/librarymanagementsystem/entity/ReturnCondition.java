package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum ReturnCondition implements EnumClass<String> {

    GOOD("GOOD", "Tốt / Bình thường"),
    DAMAGED("DAMAGED", "Hư hỏng"),
    LOST("LOST", "Bị mất");

    private final String id;
    private final String caption;

    ReturnCondition(String id, String caption) {
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
    public static ReturnCondition fromId(String id) {
        for (ReturnCondition condition : ReturnCondition.values()) {
            if (condition.getId().equals(id)) {
                return condition;
            }
        }
        return null;
    }
}
