package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "Manager Role", code = ManagerRole.CODE)
public interface ManagerRole {

    String CODE = "manager-role";

    @MenuPolicy(menuIds = {
            "ManagerDashboardView",
            "OpacCatalogView",
            "managerOperations",
            "managerLibrarySettings",
            "managerFineWaives",
            "managerExceptions",
            "managerAuditLogs",
            "LibrarySettingView",
            "FineWaiveApprovalView",
            "ExceptionHandlingView",
            "AuditLogListView",
            "MemberType.list",
            "librarianManagement",
            "Loan.list",
            "Reservation.list",
            "Fine.list",
            "Member.list",
            "administration",
            "User.list"
    })
    @ViewPolicy(viewIds = {
            "ManagerDashboardView",
            "LibrarySettingView",
            "FineWaiveApprovalView",
            "ExceptionHandlingView",
            "AuditLogListView",
            "OpacCatalogView",
            "MemberType.list", "MemberType.detail",
            "Loan.list", "Loan.detail",
            "Reservation.list",
            "Fine.list",
            "Member.list", "Member.detail", "LibraryMember.detail",
            "User.list", "User.detail"
    })

    // Catalog & Books: Manager có quyền xem danh mục
    @EntityPolicy(entityClass = Book.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Book.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Location.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Location.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Author.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Author.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Category.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Category.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Publisher.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Publisher.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    // Copies: Read & Update để duyệt thanh lý bản sao (M08)
    @EntityPolicy(entityClass = BookCopy.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = BookCopy.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    // Members: Read & Update để mở khóa độc giả bị BLOCKED (M06)
    @EntityPolicy(entityClass = Member.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = Member.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    // MemberType: Toàn quyền quản lý chính sách loại độc giả (M02)
    @EntityPolicy(entityClass = MemberType.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = MemberType.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    // User: Quản lý tài khoản nhân viên Thủ thư và Biên mục (M07)
    @EntityPolicy(entityClass = User.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = User.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    // Loan & LoanItem: Giám sát mượn trả, xử lý ngoại lệ hủy phiếu sai (M06)
    @EntityPolicy(entityClass = Loan.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = Loan.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = LoanItem.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = LoanItem.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Renewal.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Renewal.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    // Fine & Payment: Giám sát phạt, duyệt miễn/giảm phạt (M05)
    @EntityPolicy(entityClass = Fine.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = Fine.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Payment.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Payment.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    // Reservation: Giám sát đặt trước sách
    @EntityPolicy(entityClass = Reservation.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = Reservation.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    // Role & Permission: Xem thông tin để phục vụ chọn vai trò nhân viên trong UserDetailView
    @EntityPolicy(entityClass = Role.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Role.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Permission.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Permission.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    // LibrarySetting: Cấu hình tham số hệ thống (M03)
    @EntityPolicy(entityClass = LibrarySetting.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = LibrarySetting.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    // AuditLog: Nhật ký kiểm toán nghiệp vụ (M09)
    @EntityPolicy(entityClass = AuditLog.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = AuditLog.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    void managerPermissions();
}
