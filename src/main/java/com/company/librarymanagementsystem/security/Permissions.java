package com.company.librarymanagementsystem.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Permissions {

    private Permissions() {
    }

    // BOOK
    public static final String BOOK_VIEW = "BOOK_VIEW";
    public static final String BOOK_CREATE = "BOOK_CREATE";
    public static final String BOOK_EDIT = "BOOK_EDIT";
    public static final String BOOK_DELETE = "BOOK_DELETE";
    public static final String BOOK_IMPORT = "BOOK_IMPORT";
    public static final String BOOK_EXPORT = "BOOK_EXPORT";
    public static final String AUTHOR_MANAGE = "AUTHOR_MANAGE";
    public static final String CATEGORY_MANAGE = "CATEGORY_MANAGE";
    public static final String PUBLISHER_MANAGE = "PUBLISHER_MANAGE";
    public static final String LOCATION_MANAGE = "LOCATION_MANAGE";
    public static final String COPY_VIEW = "COPY_VIEW";
    public static final String COPY_CREATE = "COPY_CREATE";
    public static final String COPY_EDIT = "COPY_EDIT";
    public static final String COPY_WITHDRAW = "COPY_WITHDRAW";

    // MEMBER
    public static final String MEMBER_VIEW = "MEMBER_VIEW";
    public static final String MEMBER_CREATE = "MEMBER_CREATE";
    public static final String MEMBER_EDIT = "MEMBER_EDIT";
    public static final String MEMBER_LOCK = "MEMBER_LOCK";
    public static final String MEMBER_CARD_RENEW = "MEMBER_CARD_RENEW";
    public static final String MEMBER_TYPE_MANAGE = "MEMBER_TYPE_MANAGE";

    // LOAN
    public static final String LOAN_VIEW = "LOAN_VIEW";
    public static final String LOAN_CREATE = "LOAN_CREATE";
    public static final String LOAN_RETURN = "LOAN_RETURN";
    public static final String LOAN_RENEW = "LOAN_RENEW";
    public static final String RESERVATION_VIEW = "RESERVATION_VIEW";
    public static final String RESERVATION_MANAGE = "RESERVATION_MANAGE";
    public static final String RESERVATION_CANCEL = "RESERVATION_CANCEL";

    // FINE
    public static final String FINE_VIEW = "FINE_VIEW";
    public static final String FINE_CREATE = "FINE_CREATE";
    public static final String FINE_PAYMENT = "FINE_PAYMENT";
    public static final String FINE_WAIVE = "FINE_WAIVE";

    // REPORT
    public static final String REPORT_VIEW = "REPORT_VIEW";
    public static final String REPORT_OVERDUE = "REPORT_OVERDUE";
    public static final String REPORT_REVENUE = "REPORT_REVENUE";
    public static final String REPORT_INVENTORY = "REPORT_INVENTORY";
    public static final String REPORT_EXPORT = "REPORT_EXPORT";

    // SYSTEM
    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String ROLE_MANAGE = "ROLE_MANAGE";
    public static final String SETTING_MANAGE = "SETTING_MANAGE";
    public static final String HOLIDAY_MANAGE = "HOLIDAY_MANAGE";
    public static final String NOTIFICATION_MANAGE = "NOTIFICATION_MANAGE";
    public static final String AUDIT_LOG_VIEW = "AUDIT_LOG_VIEW";

    // PERMISSION SETS
    public static final Set<String> BOOK_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Set.of(
            BOOK_VIEW, BOOK_CREATE, BOOK_EDIT, BOOK_DELETE, BOOK_IMPORT, BOOK_EXPORT,
            AUTHOR_MANAGE, CATEGORY_MANAGE, PUBLISHER_MANAGE, LOCATION_MANAGE,
            COPY_VIEW, COPY_CREATE, COPY_EDIT, COPY_WITHDRAW
    )));

    public static final Set<String> MEMBER_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Set.of(
            MEMBER_VIEW, MEMBER_CREATE, MEMBER_EDIT, MEMBER_LOCK, MEMBER_CARD_RENEW, MEMBER_TYPE_MANAGE
    )));

    public static final Set<String> LOAN_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Set.of(
            LOAN_VIEW, LOAN_CREATE, LOAN_RETURN, LOAN_RENEW,
            RESERVATION_VIEW, RESERVATION_MANAGE, RESERVATION_CANCEL
    )));

    public static final Set<String> FINE_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Set.of(
            FINE_VIEW, FINE_CREATE, FINE_PAYMENT, FINE_WAIVE
    )));

    public static final Set<String> REPORT_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Set.of(
            REPORT_VIEW, REPORT_OVERDUE, REPORT_REVENUE, REPORT_INVENTORY, REPORT_EXPORT
    )));

    public static final Set<String> SYSTEM_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Set.of(
            USER_VIEW, USER_MANAGE, ROLE_MANAGE, SETTING_MANAGE, HOLIDAY_MANAGE,
            NOTIFICATION_MANAGE, AUDIT_LOG_VIEW
    )));

    public static final Set<String> ALL_PERMISSIONS;

    static {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(BOOK_PERMISSIONS);
        all.addAll(MEMBER_PERMISSIONS);
        all.addAll(LOAN_PERMISSIONS);
        all.addAll(FINE_PERMISSIONS);
        all.addAll(REPORT_PERMISSIONS);
        all.addAll(SYSTEM_PERMISSIONS);
        ALL_PERMISSIONS = Collections.unmodifiableSet(all);
    }
}
