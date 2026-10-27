package com.company.librarymanagementsystem.view.permission;

import com.company.librarymanagementsystem.entity.Permission;
import com.company.librarymanagementsystem.service.PermissionDataInitializer;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "permissions", layout = MainView.class)
@ViewController(id = "Permission.list")
@ViewDescriptor(path = "permission-list-view.xml")
@LookupComponent("permissionsDataGrid")
@DialogMode(width = "64em")
public class PermissionListView extends StandardListView<Permission> {

    @ViewComponent
    private CollectionLoader<Permission> permissionsDl;

    @ViewComponent
    private Select<String> moduleFilterSelect;

    @ViewComponent
    private io.jmix.flowui.component.grid.DataGrid<Permission> permissionsDataGrid;

    @ViewComponent
    private com.vaadin.flow.component.button.Button createButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button editButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button removeButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button importDefaultsButton;

    @Autowired
    private com.company.librarymanagementsystem.security.AccessControlService accessControlService;

    @Autowired
    private PermissionDataInitializer permissionDataInitializer;

    @Autowired
    private ToastService toastService;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);
        accessControlService.applyPermission(permissionsDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);

        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);
        accessControlService.applyPermission(permissionsDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);

        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);
        accessControlService.applyPermission(permissionsDataGrid.getAction("removeAction"), com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);

        accessControlService.applyPermission(importDefaultsButton, com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);
    }

    @Install(to = "permissionsDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void permissionsDataGridRemoveAfterActionPerformed(final io.jmix.flowui.util.RemoveOperation.AfterActionPerformedEvent<Permission> event) {
        toastService.success("Xóa quyền hạn thành công!");
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        initModuleFilterSelect();
    }

    private void initModuleFilterSelect() {
        moduleFilterSelect.setItems(List.of("ALL", "BOOK", "MEMBER", "LOAN", "FINE", "REPORT", "SYSTEM"));
        moduleFilterSelect.setItemLabelGenerator(this::getModuleLabel);
        moduleFilterSelect.setRenderer(new ComponentRenderer<>(mod -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.getStyle().set("gap", "8px");

            if (!"ALL".equalsIgnoreCase(mod)) {
                Span dot = new Span();
                dot.addClassName("perm-badge-dot");
                dot.addClassName("perm-dot-" + mod.toLowerCase());
                row.add(dot);
            }
            Span text = new Span(getModuleLabel(mod));
            row.add(text);
            return row;
        }));
        moduleFilterSelect.setValue("ALL");
    }

    private String getModuleLabel(String mod) {
        return switch (mod != null ? mod : "") {
            case "ALL" -> "Tất cả phân hệ";
            case "BOOK" -> "Sách & Danh mục";
            case "MEMBER" -> "Độc giả & Thẻ";
            case "LOAN" -> "Mượn / Trả";
            case "FINE" -> "Tiền phạt";
            case "REPORT" -> "Báo cáo & Thống kê";
            case "SYSTEM" -> "Hệ thống & Phân quyền";
            default -> mod != null ? mod : "";
        };
    }

    @Subscribe("moduleFilterSelect")
    public void onModuleFilterSelectValueChange(final ComponentValueChangeEvent<Select<String>, String> event) {
        String selectedModule = event.getValue();
        if (selectedModule == null || "ALL".equalsIgnoreCase(selectedModule)) {
            permissionsDl.setQuery("select e from Permission e order by e.module, e.code");
            permissionsDl.removeParameter("module");
        } else {
            permissionsDl.setQuery("select e from Permission e where e.module = :module order by e.code");
            permissionsDl.setParameter("module", selectedModule);
        }
        permissionsDl.load();
    }

    @Subscribe("importDefaultsButton")
    public void onImportDefaultsButtonClick(final ClickEvent<JmixButton> event) {
        int count = permissionDataInitializer.importPermissionsFromJson();
        permissionDataInitializer.assignDefaultPermissionsToRoles();
        permissionsDl.load();
        toastService.success(String.format("Đã nạp thành công %d quyền mặc định vào cơ sở dữ liệu!", count));
    }

    @Supply(to = "permissionsDataGrid.name", subject = "renderer")
    public Renderer<Permission> nameRenderer() {
        return new ComponentRenderer<>(permission -> {
            Span nameSpan = new Span(permission.getName() != null ? permission.getName() : "");
            nameSpan.getStyle().set("font-weight", "600");
            nameSpan.getStyle().set("color", "var(--lumo-header-text-color)");
            return nameSpan;
        });
    }

    @Supply(to = "permissionsDataGrid.code", subject = "renderer")
    public Renderer<Permission> codeRenderer() {
        return new ComponentRenderer<>(permission -> {
            Span badge = new Span();
            String code = permission.getCode();

            Span dot = new Span();
            dot.addClassName("perm-badge-dot");

            Span text = new Span(code != null ? code : "");

            badge.add(dot, text);
            badge.addClassName("perm-code-badge");

            String module = resolveModule(permission);
            badge.addClassName("perm-badge-" + module.toLowerCase());

            return badge;
        });
    }

    private String resolveModule(Permission permission) {
        if (permission.getModule() != null && !permission.getModule().isBlank()) {
            return permission.getModule().trim().toUpperCase();
        }
        String code = permission.getCode();
        if (code != null) {
            if (code.startsWith("BOOK_") || code.startsWith("AUTHOR_") || code.startsWith("CATEGORY_")
                    || code.startsWith("PUBLISHER_") || code.startsWith("LOCATION_") || code.startsWith("COPY_")) {
                return "BOOK";
            }
            if (code.startsWith("MEMBER_")) {
                return "MEMBER";
            }
            if (code.startsWith("LOAN_") || code.startsWith("RESERVATION_")) {
                return "LOAN";
            }
            if (code.startsWith("FINE_")) {
                return "FINE";
            }
            if (code.startsWith("REPORT_")) {
                return "REPORT";
            }
            if (code.startsWith("USER_") || code.startsWith("ROLE_") || code.startsWith("SETTING_")
                    || code.startsWith("HOLIDAY_") || code.startsWith("NOTIFICATION_") || code.startsWith("AUDIT_")) {
                return "SYSTEM";
            }
        }
        return "OTHER";
    }
}
