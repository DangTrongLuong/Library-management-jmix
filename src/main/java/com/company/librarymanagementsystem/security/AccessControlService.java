package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.entity.Permission;
import com.company.librarymanagementsystem.entity.Role;
import com.company.librarymanagementsystem.entity.User;
import com.vaadin.flow.component.Component;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.kit.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccessControlService {

    private static final Logger log = LoggerFactory.getLogger(AccessControlService.class);

    private final CurrentAuthentication currentAuthentication;
    private final DataManager dataManager;

    public AccessControlService(CurrentAuthentication currentAuthentication, DataManager dataManager) {
        this.currentAuthentication = currentAuthentication;
        this.dataManager = dataManager;
    }

    public boolean isAdmin() {
        if (currentAuthentication == null || !currentAuthentication.isSet()) {
            return false;
        }

        try {
            UserDetails userDetails = currentAuthentication.getUser();
            if (userDetails != null) {
                if ("admin".equalsIgnoreCase(userDetails.getUsername())) {
                    return true;
                }
                if (userDetails instanceof User appUser) {
                    if (appUser.getRole() != null && "ADMIN".equalsIgnoreCase(appUser.getRole().getCode())) {
                        return true;
                    }
                }
            }

            Authentication auth = currentAuthentication.getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                for (GrantedAuthority ga : auth.getAuthorities()) {
                    String a = ga.getAuthority();
                    if (a != null && (a.equalsIgnoreCase("ROLE_ADMIN")
                            || a.equalsIgnoreCase("ADMIN")
                            || a.equalsIgnoreCase(FullAccessRole.CODE)
                            || a.contains("system-full-access"))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi kiểm tra quyền admin: {}", e.getMessage());
        }

        return false;
    }

    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return true;
        }

        if (isAdmin()) {
            return true;
        }

        // 1. Kiểm tra nhanh qua Spring Security GrantedAuthorities
        try {
            Authentication auth = currentAuthentication.getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                String permPrefixed = "PERM_" + permissionCode.toUpperCase();
                for (GrantedAuthority ga : auth.getAuthorities()) {
                    String a = ga.getAuthority();
                    if (a != null && (a.equalsIgnoreCase(permissionCode) || a.equalsIgnoreCase(permPrefixed))) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Kiểm tra trực tiếp từ User và Role trong cơ sở dữ liệu
        try {
            UserDetails userDetails = currentAuthentication.getUser();
            if (userDetails != null) {
                User user = dataManager.load(User.class)
                        .query("select u from User u where u.username = :username")
                        .parameter("username", userDetails.getUsername())
                        .fetchPlan(builder -> builder
                                .add("role", rolePlan -> rolePlan.addFetchPlan(io.jmix.core.FetchPlan.BASE).add("permissions", io.jmix.core.FetchPlan.BASE))
                                .add("permissions", io.jmix.core.FetchPlan.BASE))
                        .optional()
                        .orElse(null);

                if (user != null) {
                    // Quyền cá nhân trực tiếp của user
                    if (user.getPermissions() != null) {
                        for (Permission p : user.getPermissions()) {
                            if (p.getCode() != null && p.getCode().equalsIgnoreCase(permissionCode)) {
                                return true;
                            }
                        }
                    }

                    // Quyền kế thừa từ Role của user
                    if (user.getRole() != null && user.getRole().getPermissions() != null) {
                        for (Permission p : user.getRole().getPermissions()) {
                            if (p.getCode() != null && p.getCode().equalsIgnoreCase(permissionCode)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi kiểm tra mã quyền {}: {}", permissionCode, e.getMessage());
        }

        return false;
    }

    /**
     * Kiểm tra người dùng có ít nhất một trong các quyền được chỉ định hay không.
     */
    public boolean hasAnyPermission(String... permissionCodes) {
        if (isAdmin()) {
            return true;
        }
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        for (String code : permissionCodes) {
            if (hasPermission(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra người dùng có đầy đủ tất cả các quyền được chỉ định hay không.
     */
    public boolean hasAllPermissions(String... permissionCodes) {
        if (isAdmin()) {
            return true;
        }
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        for (String code : permissionCodes) {
            if (!hasPermission(code)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lấy toàn bộ danh sách mã quyền hiện tại của người dùng.
     */
    public Set<String> getCurrentUserPermissionCodes() {
        if (isAdmin()) {
            return Permissions.ALL_PERMISSIONS;
        }

        Set<String> codes = new HashSet<>();
        try {
            UserDetails userDetails = currentAuthentication.getUser();
            if (userDetails != null) {
                User user = dataManager.load(User.class)
                        .query("select u from User u where u.username = :username")
                        .parameter("username", userDetails.getUsername())
                        .fetchPlan(builder -> builder
                                .add("role", rolePlan -> rolePlan.addFetchPlan(io.jmix.core.FetchPlan.BASE).add("permissions", io.jmix.core.FetchPlan.BASE))
                                .add("permissions", io.jmix.core.FetchPlan.BASE))
                        .optional()
                        .orElse(null);

                if (user != null) {
                    if (user.getPermissions() != null) {
                        for (Permission p : user.getPermissions()) {
                            if (p.getCode() != null)
                                codes.add(p.getCode().toUpperCase());
                        }
                    }
                    if (user.getRole() != null && user.getRole().getPermissions() != null) {
                        for (Permission p : user.getRole().getPermissions()) {
                            if (p.getCode() != null)
                                codes.add(p.getCode().toUpperCase());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi lấy danh sách quyền của người dùng: {}", e.getMessage());
        }
        return codes;
    }

    /**
     * Ẩn/Hiện một Component giao diện dựa theo mã quyền.
     */
    public void applyPermission(Component component, String permissionCode) {
        if (component != null) {
            component.setVisible(hasPermission(permissionCode));
        }
    }

    /**
     * Ẩn/Hiện và Kích hoạt một Action (nút bấm DataGrid) dựa theo mã quyền.
     */
    public void applyPermission(Action action, String permissionCode) {
        if (action != null) {
            boolean permitted = hasPermission(permissionCode);
            action.setVisible(permitted);
            action.setEnabled(permitted);
        }
    }

    /**
     * Ẩn/Hiện Component giao diện nếu người dùng có ít nhất một quyền trong danh
     * sách.
     */
    public void applyAnyPermission(Component component, String... permissionCodes) {
        if (component != null) {
            component.setVisible(hasAnyPermission(permissionCodes));
        }
    }

    /**
     * Ẩn/Hiện và Kích hoạt Action nếu người dùng có ít nhất một quyền trong danh
     * sách.
     */
    public void applyAnyPermission(Action action, String... permissionCodes) {
        if (action != null) {
            boolean permitted = hasAnyPermission(permissionCodes);
            action.setVisible(permitted);
            action.setEnabled(permitted);
        }
    }

    // --- Module BOOK ---
    public boolean canViewBook() {
        return hasPermission(Permissions.BOOK_VIEW);
    }

    public boolean canCreateBook() {
        return hasPermission(Permissions.BOOK_CREATE);
    }

    public boolean canEditBook() {
        return hasPermission(Permissions.BOOK_EDIT);
    }

    public boolean canDeleteBook() {
        return hasPermission(Permissions.BOOK_DELETE);
    }

    public boolean canImportBook() {
        return hasPermission(Permissions.BOOK_IMPORT);
    }

    public boolean canExportBook() {
        return hasPermission(Permissions.BOOK_EXPORT);
    }

    public boolean canManageAuthor() {
        return hasPermission(Permissions.AUTHOR_MANAGE);
    }

    public boolean canManageCategory() {
        return hasPermission(Permissions.CATEGORY_MANAGE);
    }

    public boolean canManagePublisher() {
        return hasPermission(Permissions.PUBLISHER_MANAGE);
    }

    public boolean canManageLocation() {
        return hasPermission(Permissions.LOCATION_MANAGE);
    }

    public boolean canViewCopy() {
        return hasPermission(Permissions.COPY_VIEW);
    }

    public boolean canCreateCopy() {
        return hasPermission(Permissions.COPY_CREATE);
    }

    public boolean canEditCopy() {
        return hasPermission(Permissions.COPY_EDIT);
    }

    public boolean canWithdrawCopy() {
        return hasPermission(Permissions.COPY_WITHDRAW);
    }

    // --- Module MEMBER ---
    public boolean canViewMember() {
        return hasPermission(Permissions.MEMBER_VIEW);
    }

    public boolean canCreateMember() {
        return hasPermission(Permissions.MEMBER_CREATE);
    }

    public boolean canEditMember() {
        return hasPermission(Permissions.MEMBER_EDIT);
    }

    public boolean canLockMember() {
        return hasPermission(Permissions.MEMBER_LOCK);
    }

    public boolean canRenewMemberCard() {
        return hasPermission(Permissions.MEMBER_CARD_RENEW);
    }

    public boolean canManageMemberType() {
        return hasPermission(Permissions.MEMBER_TYPE_MANAGE);
    }

    // --- Module LOAN ---
    public boolean canViewLoan() {
        return hasPermission(Permissions.LOAN_VIEW);
    }

    public boolean canCreateLoan() {
        return hasPermission(Permissions.LOAN_CREATE);
    }

    public boolean canReturnLoan() {
        return hasPermission(Permissions.LOAN_RETURN);
    }

    public boolean canRenewLoan() {
        return hasPermission(Permissions.LOAN_RENEW);
    }

    public boolean canViewReservation() {
        return hasPermission(Permissions.RESERVATION_VIEW);
    }

    public boolean canManageReservation() {
        return hasPermission(Permissions.RESERVATION_MANAGE);
    }

    public boolean canCancelReservation() {
        return hasPermission(Permissions.RESERVATION_CANCEL);
    }

    // --- Module FINE ---
    public boolean canViewFine() {
        return hasPermission(Permissions.FINE_VIEW);
    }

    public boolean canCreateFine() {
        return hasPermission(Permissions.FINE_CREATE);
    }

    public boolean canPayFine() {
        return hasPermission(Permissions.FINE_PAYMENT);
    }

    public boolean canWaiveFine() {
        return hasPermission(Permissions.FINE_WAIVE);
    }

    // --- Module REPORT ---
    public boolean canViewReport() {
        return hasPermission(Permissions.REPORT_VIEW);
    }

    public boolean canViewOverdueReport() {
        return hasPermission(Permissions.REPORT_OVERDUE);
    }

    public boolean canViewRevenueReport() {
        return hasPermission(Permissions.REPORT_REVENUE);
    }

    public boolean canViewInventoryReport() {
        return hasPermission(Permissions.REPORT_INVENTORY);
    }

    public boolean canExportReport() {
        return hasPermission(Permissions.REPORT_EXPORT);
    }

    // --- Module SYSTEM ---
    public boolean canViewUser() {
        return hasPermission(Permissions.USER_VIEW);
    }

    public boolean canManageUser() {
        return hasPermission(Permissions.USER_MANAGE);
    }

    public boolean canManageRole() {
        return hasPermission(Permissions.ROLE_MANAGE);
    }

    public boolean canManageSetting() {
        return hasPermission(Permissions.SETTING_MANAGE);
    }

    public boolean canManageHoliday() {
        return hasPermission(Permissions.HOLIDAY_MANAGE);
    }

    public boolean canManageNotification() {
        return hasPermission(Permissions.NOTIFICATION_MANAGE);
    }

    public boolean canViewAuditLog() {
        return hasPermission(Permissions.AUDIT_LOG_VIEW);
    }
}
