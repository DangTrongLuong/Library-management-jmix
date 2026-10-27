package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum BookCopyCondition implements EnumClass<String> {

    NEW("NEW", "Mới 100%"),
    GOOD("GOOD", "Tốt"),
    FAIR("FAIR", "Trung bình / Sờn cũ"),
    POOR("POOR", "Kém / Cần bảo quản");

    private final String id;
    private final String caption;

    BookCopyCondition(String id, String caption) {
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
    public static BookCopyCondition fromId(String id) {
        for (BookCopyCondition condition : BookCopyCondition.values()) {
            if (condition.getId().equals(id)) {
                return condition;
            }
        }
        return null;
    }
}
