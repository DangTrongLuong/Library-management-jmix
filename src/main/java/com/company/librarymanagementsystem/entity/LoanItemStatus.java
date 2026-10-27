package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum LoanItemStatus implements EnumClass<String> {

    BORROWED("BORROWED", "Đang mượn"),
    RETURNED("RETURNED", "Đã trả"),
    LOST("LOST", "Đã mất");

    private final String id;
    private final String caption;

    LoanItemStatus(String id, String caption) {
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
    public static LoanItemStatus fromId(String id) {
        for (LoanItemStatus status : LoanItemStatus.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        return null;
    }
}
