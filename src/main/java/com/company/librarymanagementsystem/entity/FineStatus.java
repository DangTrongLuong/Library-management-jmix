package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum FineStatus implements EnumClass<String> {

    UNPAID("UNPAID", "Chưa thanh toán"),
    PARTIAL("PARTIAL", "Đã trả một phần"),
    PAID("PAID", "Đã thanh toán đủ"),
    WAIVED("WAIVED", "Đã được miễn giảm");

    private final String id;
    private final String caption;

    FineStatus(String id, String caption) {
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
    public static FineStatus fromId(String id) {
        for (FineStatus status : FineStatus.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        return null;
    }
}
