package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum FineType implements EnumClass<String> {

    OVERDUE("OVERDUE", "Phạt quá hạn"),
    LOST("LOST", "Bồi thường mất sách"),
    DAMAGED("DAMAGED", "Bồi thường hỏng sách"),
    CARD_FEE("CARD_FEE", "Phí làm/cấp lại thẻ"),
    OTHER("OTHER", "Phí / phạt khác");

    private final String id;
    private final String caption;

    FineType(String id, String caption) {
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
    public static FineType fromId(String id) {
        for (FineType type : FineType.values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }
}
