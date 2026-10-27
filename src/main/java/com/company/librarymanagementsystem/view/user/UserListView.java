package com.company.librarymanagementsystem.view.user;

import com.company.librarymanagementsystem.entity.Member;
import com.company.librarymanagementsystem.entity.User;
import com.company.librarymanagementsystem.entity.UserStatus;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.service.UserService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Route(value = "users", layout = MainView.class)
@ViewController(id = "User.list")
@ViewDescriptor(path = "user-list-view.xml")
@LookupComponent("usersDataGrid")
@DialogMode(width = "64em")
public class UserListView extends StandardListView<User> {

    @ViewComponent
    private CollectionLoader<User> usersDl;

    @ViewComponent
    private DataGrid<User> usersDataGrid;

    @ViewComponent
    private Button createButton;

    @ViewComponent
    private Button editButton;

    @ViewComponent
    private Button removeButton;

    @ViewComponent
    private Button resetPasswordBtn;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private UserService userService;

    @Autowired
    private ToastService toastService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.company.librarymanagementsystem.security.AccessControlService accessControlService;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.USER_MANAGE);
        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.USER_MANAGE);
        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.USER_MANAGE);
        accessControlService.applyPermission(resetPasswordBtn, com.company.librarymanagementsystem.security.Permissions.USER_MANAGE);
        accessControlService.applyPermission(usersDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.USER_MANAGE);
        accessControlService.applyPermission(usersDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.USER_MANAGE);

        usersDataGrid.addSelectionListener(e -> updateActionButtonsState());
        updateActionButtonsState();
    }

    private void updateActionButtonsState() {
        boolean canManage = accessControlService.canManageUser();
        User selected = usersDataGrid.getSingleSelectedItem();
        boolean hasSelection = (selected != null);
        resetPasswordBtn.setEnabled(canManage && hasSelection);
        removeButton.setEnabled(canManage && hasSelection);
    }

    @Subscribe("removeButton")
    public void onRemoveButtonClick(final ClickEvent<Button> event) {
        User selected = usersDataGrid.getSingleSelectedItem();
        if (selected == null) {
            return;
        }

        UserService.UserRelatedSummary summary = userService.inspectUserRelatedData(selected.getId());
        if (summary == null) {
            toastService.error("Không tìm thấy thông tin người dùng!");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Xác nhận xóa người dùng & dữ liệu liên quan");
        dialog.setWidth("580px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        // Khối thông tin người dùng
        Div userCard = new Div();
        userCard.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "12px 16px")
                .set("border-radius", "8px")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        String roleName = selected.getRole() != null ? selected.getRole().getName() : "Chưa phân vai trò";
        Div nameRow = new Div();
        nameRow.getStyle().set("font-weight", "600").set("font-size", "16px").set("margin-bottom", "4px");
        nameRow.setText(String.format("%s (%s)",
                selected.getFullName() != null ? selected.getFullName() : selected.getUsername(),
                selected.getUsername()
        ));

        Div infoRow = new Div();
        infoRow.getStyle().set("font-size", "13px").set("color", "var(--lumo-secondary-text-color)");
        infoRow.setText(String.format("Email: %s | Vai trò: %s",
                selected.getEmail() != null ? selected.getEmail() : "—",
                roleName
        ));

        userCard.add(nameRow, infoRow);
        content.add(userCard);

        // Khối liệt kê dữ liệu liên quan
        Div detailBox = new Div();
        detailBox.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "8px").set("width", "100%");

        if (summary.hasReaderData()) {
            Member m = summary.member();
            Div memberHeader = new Div();
            memberHeader.getStyle().set("font-weight", "600").set("color", "var(--lumo-primary-text-color)");
            memberHeader.setText(String.format("Hồ sơ Độc giả liên kết: Thẻ %s (%s)", m.getMemberCode(), m.getFullName()));
            detailBox.add(memberHeader);

            Div readerList = new Div();
            readerList.getStyle().set("padding-left", "8px").set("display", "flex").set("flex-direction", "column").set("gap", "4px").set("font-size", "14px");

            if (summary.borrowedBooksCount() > 0) {
                Div alertBorrow = new Div();
                alertBorrow.getStyle().set("color", "#b91c1c").set("font-weight", "600");
                alertBorrow.setText(String.format("• Sách đang mượn: %d cuốn -> Sẽ tự động hoàn trả về kho ở trạng thái Sẵn sàng (AVAILABLE).", summary.borrowedBooksCount()));
                readerList.add(alertBorrow);
            }

            if (summary.totalLoansCount() > 0) {
                readerList.add(new Span(String.format("• Lịch sử phiếu mượn: %d phiếu -> Sẽ bị xóa vĩnh viễn.", summary.totalLoansCount())));
            }

            if (summary.unpaidFinesTotal().compareTo(BigDecimal.ZERO) > 0 || summary.totalFinesCount() > 0) {
                readerList.add(new Span(String.format("• Tiền phạt & Phí: %d khoản (Chưa thu: %,d VNĐ) -> Sẽ bị xóa vĩnh viễn.",
                        summary.totalFinesCount(), summary.unpaidFinesTotal().longValue())));
            }

            if (summary.pendingReservationsCount() > 0) {
                readerList.add(new Span(String.format("• Lượt đặt trước sách: %d lượt -> Sẽ bị hủy và giải phóng sách giữ chỗ.", summary.pendingReservationsCount())));
            }

            detailBox.add(readerList);
        }

        if (summary.hasStaffData()) {
            Div staffAlert = new Div();
            staffAlert.getStyle().set("background-color", "#eff6ff").set("color", "#1d4ed8").set("padding", "8px 12px").set("border-radius", "6px").set("font-size", "13px");
            staffAlert.setText(String.format("• Lịch sử nghiệp vụ thư viện (%d phiếu mượn, %d sách nhận trả, %d khoản phạt, %d khoản thu) sẽ được tự động bàn giao sang tài khoản Quản trị viên (Admin) để bảo toàn dữ liệu thống kê của thư viện.",
                    summary.staffIssuedLoansCount(), summary.staffReceivedItemsCount(), summary.staffFinesCount(), summary.staffPaymentsCount()));
            detailBox.add(staffAlert);
        }

        content.add(detailBox);

        // Khối cảnh báo đỏ nguy hiểm
        Div warningBox = new Div();
        warningBox.getStyle()
                .set("background-color", "#fef2f2")
                .set("border", "1px solid #fecaca")
                .set("color", "#991b1b")
                .set("padding", "10px 14px")
                .set("border-radius", "8px")
                .set("font-size", "13px")
                .set("font-weight", "500")
                .set("line-height", "1.4");
        warningBox.setText("CẢNH BÁO NGUY HIỂM: Hành động này sẽ xóa vĩnh viễn tài khoản người dùng và toàn bộ dữ liệu hồ sơ liên quan đã nêu ở trên. Bạn sẽ không thể khôi phục lại dữ liệu sau khi xóa!");
        content.add(warningBox);

        dialog.add(content);

        Button cancelBtn = new Button("Hủy bỏ", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button confirmBtn = new Button("Xác nhận xóa tất cả", VaadinIcon.TRASH.create(), e -> {
            try {
                userService.deleteUserAndAllRelatedData(selected.getId());
                toastService.success(String.format("Đã xóa người dùng '%s' và toàn bộ dữ liệu liên quan thành công!", selected.getUsername()));
                dialog.close();
                usersDl.load();
                updateActionButtonsState();
            } catch (Exception ex) {
                toastService.error("Không thể xóa người dùng: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    @Subscribe("resetPasswordBtn")
    public void onResetPasswordBtnClick(final ClickEvent<Button> event) {
        User selected = usersDataGrid.getSingleSelectedItem();
        if (selected == null) {
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Xác nhận đặt lại mật khẩu");
        dialog.setText(String.format(
                "Bạn có chắc chắn muốn đặt lại mật khẩu của người dùng '%s' (%s)? Mật khẩu sẽ đặt về mặc định 123456a@",
                selected.getUsername(),
                selected.getFullName() != null ? selected.getFullName() : ""
        ));
        dialog.setCancelable(true);
        dialog.setCancelText("Hủy bỏ");
        dialog.setConfirmText("Xác nhận đổi");
        dialog.setConfirmButtonTheme("primary");
        dialog.addConfirmListener(e -> {
            try {
                User userToUpdate = dataManager.load(User.class).id(selected.getId()).one();
                userToUpdate.setPassword(passwordEncoder.encode("123456a@"));
                userToUpdate.setMustChangePassword(true);
                dataManager.save(userToUpdate);

                toastService.success(String.format("Đã đổi mật khẩu tài khoản '%s' về mặc định 123456a@ thành công!", selected.getUsername()));
            } catch (Exception ex) {
                toastService.error("Lỗi khi đổi mật khẩu: " + ex.getMessage());
            }
        });
        dialog.open();
    }

    @Supply(to = "usersDataGrid.status", subject = "renderer")
    public Renderer<User> statusRenderer() {
        return new ComponentRenderer<>(user -> {
            Span badge = new Span();
            UserStatus status = user.getStatus();
            String statusId = status != null ? status.getId() : "";

            if (UserStatus.ACTIVE.getId().equalsIgnoreCase(statusId)) {
                badge.setText("Đang hoạt động");
                badge.getElement().getThemeList().add("badge success");
            } else if (UserStatus.LOCKED.getId().equalsIgnoreCase(statusId)) {
                badge.setText("Bị khóa");
                badge.getElement().getThemeList().add("badge error");
            } else if (UserStatus.INACTIVE.getId().equalsIgnoreCase(statusId)) {
                badge.setText("Ngừng hoạt động");
                badge.getElement().getThemeList().add("badge contrast");
            } else {
                badge.setText(statusId.isBlank() ? "—" : statusId);
                badge.getElement().getThemeList().add("badge contrast");
            }
            return badge;
        });
    }

    @Supply(to = "usersDataGrid.role", subject = "renderer")
    public Renderer<User> roleRenderer() {
        return new ComponentRenderer<>(user -> {
            Span span = new Span();
            if (user.getRole() != null) {
                String roleCode = user.getRole().getCode();
                span.setText(user.getRole().getName());
                if (roleCode != null) {
                    switch (roleCode.toUpperCase()) {
                        case "ADMIN" -> span.getElement().getThemeList().add("badge error");
                        case "MANAGER" -> span.getElement().getThemeList().add("badge warning");
                        case "LIBRARIAN" -> span.getElement().getThemeList().add("badge primary");
                        case "CATALOGER" -> span.getElement().getThemeList().add("badge");
                        case "READER" -> span.getElement().getThemeList().add("badge success");
                        default -> span.getElement().getThemeList().add("badge contrast");
                    }
                } else {
                    span.getElement().getThemeList().add("badge contrast");
                }
            } else {
                span.setText("Chưa phân vai trò");
                span.getElement().getThemeList().add("badge contrast");
            }
            return span;
        });
    }
}