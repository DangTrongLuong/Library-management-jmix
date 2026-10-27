package com.company.librarymanagementsystem.view.user;

import com.company.librarymanagementsystem.entity.Permission;
import com.company.librarymanagementsystem.entity.Role;
import com.company.librarymanagementsystem.entity.User;
import com.company.librarymanagementsystem.entity.UserStatus;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

@Route(value = "users/:id", layout = MainView.class)
@ViewController(id = "User.detail")
@ViewDescriptor(path = "user-detail-view.xml")
@EditedEntityContainer("userDc")
public class UserDetailView extends StandardDetailView<User> {

    @ViewComponent
    private TypedTextField<String> usernameField;
    @ViewComponent
    private Div defaultPasswordNote;
    @ViewComponent
    private ComboBox<String> timeZoneField;
    @ViewComponent
    private Select<UserStatus> statusField;
    @ViewComponent
    private EntityComboBox<Role> roleField;
    @ViewComponent
    private ComboBox<Permission> permissionSearchBox;
    @ViewComponent
    private Div selectedPermissionsContainer;
    @ViewComponent
    private Span selectedPermCountBadge;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private EntityStates entityStates;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ToastService toastService;
    @Autowired
    private com.company.librarymanagementsystem.security.AccessControlService accessControlService;
    @Autowired
    private com.company.librarymanagementsystem.service.MemberService memberService;

    private boolean newEntity;
    private final Set<Permission> selectedPermissions = new LinkedHashSet<>();
    private final List<Permission> allSystemPermissions = new ArrayList<>();

    @Subscribe
    public void onInit(final InitEvent event) {
        setShowSaveNotification(false);

        // Đặt múi giờ mặc định Hồ Chí Minh lên đầu danh sách
        List<String> timezones = new ArrayList<>(List.of(TimeZone.getAvailableIDs()));
        timezones.remove("Asia/Ho_Chi_Minh");
        timezones.add(0, "Asia/Ho_Chi_Minh");
        timeZoneField.setItems(timezones);

        // Cấu hình lựa chọn trạng thái tài khoản
        statusField.setItems(UserStatus.values());
        statusField.setItemLabelGenerator(status -> switch (status != null ? status : UserStatus.ACTIVE) {
            case ACTIVE -> "Đang hoạt động";
            case LOCKED -> "Bị khóa";
            case INACTIVE -> "Ngừng hoạt động";
        });

        // Phân quyền chọn vai trò: Manager chỉ được gán vai trò Thủ thư và Biên mục
        List<Role> allRoles = dataManager.load(Role.class)
                .query("select r from Role r order by r.name")
                .list();
        List<Role> allowedRoles;
        if (accessControlService.isAdmin()) {
            allowedRoles = allRoles;
        } else {
            allowedRoles = allRoles.stream()
                    .filter(r -> r.getCode() != null &&
                            ("LIBRARIAN".equalsIgnoreCase(r.getCode()) || "CATALOGER".equalsIgnoreCase(r.getCode())))
                    .toList();
        }
        roleField.setItems(allowedRoles);

        // Nạp toàn bộ quyền hệ thống vào ô tìm kiếm chọn quyền
        initPermissionSearchBox();
    }

    private void initPermissionSearchBox() {
        allSystemPermissions.clear();
        allSystemPermissions.addAll(dataManager.load(Permission.class)
                .query("select p from Permission p order by p.module, p.code")
                .list());

        permissionSearchBox.setItemLabelGenerator(p -> {
            if (p == null)
                return "";
            String modLabel = getModuleShortLabel(p.getModule());
            return String.format("[%s] %s (%s)", modLabel, p.getName() != null ? p.getName() : p.getCode(),
                    p.getCode());
        });

        permissionSearchBox.setRenderer(new ComponentRenderer<>(p -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setPadding(false);
            row.getStyle().set("gap", "8px");

            Span dot = new Span();
            String mod = p.getModule() != null ? p.getModule().toLowerCase() : "other";
            dot.addClassNames("perm-badge-dot", "perm-dot-" + mod);

            Span text = new Span(permissionSearchBox.getItemLabelGenerator().apply(p));
            row.add(dot, text);
            return row;
        }));

        updatePermissionSearchBoxItems();
    }

    private void updatePermissionSearchBoxItems() {
        if (permissionSearchBox == null) {
            return;
        }
        Set<String> selectedCodes = selectedPermissions.stream()
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> selectedIds = selectedPermissions.stream()
                .map(Permission::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Permission> available = allSystemPermissions.stream()
                .filter(p -> (p.getCode() == null || !selectedCodes.contains(p.getCode()))
                        && (p.getId() == null || !selectedIds.contains(p.getId())))
                .toList();

        ComboBox.ItemFilter<Permission> itemFilter = (perm, filterString) -> {
            if (filterString == null || filterString.isBlank()) {
                return true;
            }
            String filter = filterString.trim().toLowerCase();
            String name = perm.getName() != null ? perm.getName().toLowerCase() : "";
            String code = perm.getCode() != null ? perm.getCode().toLowerCase() : "";
            String module = perm.getModule() != null ? perm.getModule().toLowerCase() : "";
            String desc = perm.getDescription() != null ? perm.getDescription().toLowerCase() : "";
            String label = getModuleShortLabel(perm.getModule()).toLowerCase();
            return name.contains(filter) || code.contains(filter) || module.contains(filter)
                    || desc.contains(filter) || label.contains(filter);
        };

        permissionSearchBox.setItems(itemFilter, available);
        if (available.isEmpty()) {
            permissionSearchBox.setPlaceholder("Đã gán toàn bộ quyền hạn hệ thống");
        } else {
            permissionSearchBox.setPlaceholder("Nhập tên, mã hoặc chọn quyền để thêm (" + available.size() + " quyền khả dụng)...");
        }
    }

    private String getModuleShortLabel(String module) {
        return switch (module != null ? module : "") {
            case "BOOK" -> "Sách";
            case "MEMBER" -> "Độc giả";
            case "LOAN" -> "Mượn/Trả";
            case "FINE" -> "Tiền phạt";
            case "REPORT" -> "Báo cáo";
            case "SYSTEM" -> "Hệ thống";
            default -> module != null ? module : "";
        };
    }

    @Subscribe("permissionSearchBox")
    public void onPermissionSearchBoxValueChange(
            final ComponentValueChangeEvent<ComboBox<Permission>, Permission> event) {
        Permission selected = event.getValue();
        if (selected != null) {
            boolean alreadySelected = selectedPermissions.stream()
                    .anyMatch(p -> Objects.equals(p.getId(), selected.getId())
                            || Objects.equals(p.getCode(), selected.getCode()));
            if (!alreadySelected) {
                selectedPermissions.add(selected);
                refreshSelectedPermissionsUi();
                toastService.info("Đã thêm: " + (selected.getName() != null ? selected.getName() : selected.getCode()));
            } else {
                toastService.warning("Quyền này đã được chọn trước đó");
            }
            permissionSearchBox.clear();
        }
    }

    private int getModuleSortWeight(String module) {
        return switch (module != null ? module : "") {
            case "BOOK" -> 1;
            case "LOAN" -> 2;
            case "FINE" -> 3;
            case "MEMBER" -> 4;
            case "REPORT" -> 5;
            case "SYSTEM" -> 6;
            default -> 99;
        };
    }

    private void refreshSelectedPermissionsUi() {
        updatePermissionSearchBoxItems();
        selectedPermissionsContainer.removeAll();
        selectedPermCountBadge.setText(selectedPermissions.size() + " quyền");

        if (selectedPermissions.isEmpty()) {
            Span emptyText = new Span(
                    "Chưa có quyền hạn nào được chọn. Hãy chọn Vai trò chính ở trên hoặc tìm quyền từ ô chọn để thêm.");
            emptyText.getStyle().set("grid-column", "1 / -1");
            emptyText.getStyle().set("text-align", "center");
            emptyText.getStyle().set("color", "var(--lumo-secondary-text-color)");
            emptyText.getStyle().set("font-style", "italic");
            emptyText.getStyle().set("font-size", "0.875rem");
            emptyText.getStyle().set("padding", "24px 0");
            selectedPermissionsContainer.add(emptyText);
            return;
        }

        // Sắp xếp các quyền theo phân hệ để các màu giống nhau luôn đi liền nhau
        List<Permission> sortedList = new ArrayList<>(selectedPermissions);
        sortedList.sort(Comparator.comparingInt((Permission p) -> getModuleSortWeight(p.getModule()))
                .thenComparing(p -> p.getName() != null ? p.getName() : ""));

        // Chia danh sách thành 2 nửa cân đối (Cột trái & Cột phải)
        int total = sortedList.size();
        int mid = (total + 1) / 2;
        List<Permission> leftList = sortedList.subList(0, mid);
        List<Permission> rightList = sortedList.subList(mid, total);

        Div leftCol = new Div();
        leftCol.addClassName("perms-column");
        for (Permission perm : leftList) {
            leftCol.add(createPermissionChip(perm));
        }

        Div rightCol = new Div();
        rightCol.addClassName("perms-column");
        for (Permission perm : rightList) {
            rightCol.add(createPermissionChip(perm));
        }

        selectedPermissionsContainer.add(leftCol, rightCol);
    }

    private Component createPermissionChip(Permission perm) {
        HorizontalLayout chip = new HorizontalLayout();
        chip.addClassName("perm-chip");
        chip.setPadding(false);
        chip.setSpacing(false);

        String module = perm.getModule() != null ? perm.getModule().toUpperCase() : "OTHER";
        chip.addClassName("perm-chip-" + module.toLowerCase());

        // Bên trái: Dot màu + Tên quyền
        HorizontalLayout content = new HorizontalLayout();
        content.addClassName("perm-chip-content");
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setSpacing(true);

        Span dot = new Span();
        dot.addClassNames("perm-badge-dot", "perm-dot-" + module.toLowerCase());

        Span text = new Span(String.format("[%s] %s (%s)", getModuleShortLabel(module),
                perm.getName() != null ? perm.getName() : perm.getCode(), perm.getCode()));
        text.addClassName("perm-chip-text");
        text.getElement().setAttribute("title", (perm.getName() != null ? perm.getName() : "") + " - "
                + (perm.getDescription() != null ? perm.getDescription() : ""));

        content.add(dot, text);

        // Bên phải: Nút xóa quyền
        Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        removeBtn.addClassName("perm-chip-remove-btn");
        removeBtn.getElement().setAttribute("title", "Xóa quyền này khỏi người dùng");
        removeBtn.addClickListener(e -> {
            selectedPermissions.removeIf(
                    p -> Objects.equals(p.getId(), perm.getId()) || Objects.equals(p.getCode(), perm.getCode()));
            refreshSelectedPermissionsUi();
        });

        chip.add(content, removeBtn);
        return chip;
    }

    @Subscribe
    public void onInitEntity(final InitEntityEvent<User> event) {
        usernameField.setReadOnly(false);
        event.getEntity().setStatus(UserStatus.ACTIVE);
        event.getEntity().setActive(true);
        event.getEntity().setTimeZoneId("Asia/Ho_Chi_Minh");
        event.getEntity().setMustChangePassword(true);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        if (entityStates.isNew(getEditedEntity()) || getEditedEntity().getTimeZoneId() == null
                || getEditedEntity().getTimeZoneId().isBlank()) {
            getEditedEntity().setTimeZoneId("Asia/Ho_Chi_Minh");
            timeZoneField.setValue("Asia/Ho_Chi_Minh");
        }

        selectedPermissions.clear();
        if (getEditedEntity().getPermissions() != null && !getEditedEntity().getPermissions().isEmpty()) {
            selectedPermissions.addAll(getEditedEntity().getPermissions());
        } else if (getEditedEntity().getRole() != null) {
            Role r = dataManager.load(Role.class)
                    .id(getEditedEntity().getRole().getId())
                    .fetchPlan(builder -> builder.add("permissions", "_base"))
                    .optional()
                    .orElse(null);
            if (r != null && r.getPermissions() != null) {
                selectedPermissions.addAll(r.getPermissions());
            }
        }
        refreshSelectedPermissionsUi();

        if (defaultPasswordNote != null) {
            defaultPasswordNote.setVisible(entityStates.isNew(getEditedEntity()));
        }
        usernameField.setReadOnly(!entityStates.isNew(getEditedEntity()));
        usernameField.setInvalid(false);
        usernameField.setErrorMessage(null);
    }

    @Subscribe("roleField")
    public void onRoleFieldValueChange(final ComponentValueChangeEvent<EntityComboBox<Role>, Role> event) {
        if (!event.isFromClient()) {
            return;
        }
        Role selectedRole = event.getValue();
        if (selectedRole != null) {
            Role roleWithPermissions = dataManager.load(Role.class)
                    .id(selectedRole.getId())
                    .fetchPlan(builder -> builder.add("permissions", "_base"))
                    .optional()
                    .orElse(selectedRole);

            if (roleWithPermissions.getPermissions() != null) {
                selectedPermissions.clear();
                selectedPermissions.addAll(roleWithPermissions.getPermissions());
                refreshSelectedPermissionsUi();
                toastService.info(String.format("Đã tự động chọn %d quyền mặc định của vai trò '%s'",
                        roleWithPermissions.getPermissions().size(), selectedRole.getName()));
            }
        }
    }

    @Subscribe("applyRolePermissionsBtn")
    public void onApplyRolePermissionsBtnClick(final ClickEvent<Button> event) {
        Role selectedRole = roleField.getValue();
        if (selectedRole == null) {
            toastService.warning("Vui lòng chọn vai trò chính trước!");
            return;
        }
        Role roleWithPermissions = dataManager.load(Role.class)
                .id(selectedRole.getId())
                .fetchPlan(builder -> builder.add("permissions", "_base"))
                .optional()
                .orElse(selectedRole);

        if (roleWithPermissions.getPermissions() != null) {
            selectedPermissions.clear();
            selectedPermissions.addAll(roleWithPermissions.getPermissions());
            refreshSelectedPermissionsUi();
            toastService.info(String.format("Đã lấy %d quyền mặc định của vai trò '%s'",
                    roleWithPermissions.getPermissions().size(), selectedRole.getName()));
        }
    }

    @Subscribe("selectAllPermissionsBtn")
    public void onSelectAllPermissionsBtnClick(final ClickEvent<Button> event) {
        List<Permission> allPerms = dataManager.load(Permission.class).all().list();
        selectedPermissions.clear();
        selectedPermissions.addAll(allPerms);
        refreshSelectedPermissionsUi();
        toastService.info(String.format("Đã chọn toàn bộ %d quyền của hệ thống", allPerms.size()));
    }

    @Subscribe("clearAllPermissionsBtn")
    public void onClearAllPermissionsBtnClick(final ClickEvent<Button> event) {
        selectedPermissions.clear();
        refreshSelectedPermissionsUi();
        toastService.info("Đã bỏ chọn tất cả quyền hạn");
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        newEntity = entityStates.isNew(getEditedEntity());
        if (newEntity) {
            getEditedEntity().setPassword(passwordEncoder.encode("123456a@"));
            getEditedEntity().setMustChangePassword(true);
        }
        boolean isActive = getEditedEntity().getStatus() == UserStatus.ACTIVE;
        getEditedEntity().setActive(isActive);
        getEditedEntity().setPermissions(new ArrayList<>(selectedPermissions));

        String lastName = getEditedEntity().getLastName();
        String firstName = getEditedEntity().getFirstName();
        String full = ((lastName != null ? lastName.trim() + " " : "") + (firstName != null ? firstName.trim() : "")).trim();
        getEditedEntity().setFullName(!full.isEmpty() ? full : null);
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        User user = getEditedEntity();
        memberService.syncMemberFromUser(user);

        if (newEntity) {
            toastService.success(String.format("Tạo mới người dùng '%s' thành công!", user.getUsername()));
            if (user.getRole() == null) {
                toastService.warning("Lưu ý: Người dùng này chưa được phân vai trò");
            }
            newEntity = false;
        } else {
            toastService.success(
                    String.format("Cập nhật thông tin người dùng '%s' thành công!", user.getUsername()));
        }
    }
}