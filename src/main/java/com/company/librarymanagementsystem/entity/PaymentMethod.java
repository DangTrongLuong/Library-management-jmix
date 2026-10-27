package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum PaymentMethod implements EnumClass<String> {

    CASH("CASH", "Tiền mặt tại quầy"),
    BANK_TRANSFER("BANK_TRANSFER", "Chuyển khoản ngân hàng"),
    CARD("CARD", "Thẻ ngân hàng / POS"),
    E_WALLET("E_WALLET", "Ví điện tử (MoMo, ZaloPay...)");

    private final String id;
    private final String caption;

    PaymentMethod(String id, String caption) {
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
    public static PaymentMethod fromId(String id) {
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.getId().equals(id)) {
                return method;
            }
        }
        return null;
    }
}
