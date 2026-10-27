package com.company.librarymanagementsystem.view.role;

import com.company.librarymanagementsystem.entity.Role;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "roles", layout = MainView.class)
@ViewController(id = "Role.list")
@ViewDescriptor(path = "role-list-view.xml")
@LookupComponent("rolesDataGrid")
@DialogMode(width = "64em")
public class RoleListView extends StandardListView<Role> {

    @ViewComponent
    private io.jmix.flowui.component.grid.DataGrid<Role> rolesDataGrid;

    @ViewComponent
    private com.vaadin.flow.component.button.Button createButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button editButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button removeButton;

    @org.springframework.beans.factory.annotation.Autowired
    private com.company.librarymanagementsystem.security.AccessControlService accessControlService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.company.librarymanagementsystem.service.ToastService toastService;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);
        accessControlService.applyPermission(rolesDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);

        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);
        accessControlService.applyPermission(rolesDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);

        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);
        accessControlService.applyPermission(rolesDataGrid.getAction("removeAction"), com.company.librarymanagementsystem.security.Permissions.ROLE_MANAGE);
    }

    @Install(to = "rolesDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void rolesDataGridRemoveAfterActionPerformed(final io.jmix.flowui.util.RemoveOperation.AfterActionPerformedEvent<Role> event) {
        toastService.success("Xóa vai trò thành công!");
    }

    @Supply(to = "rolesDataGrid.code", subject = "renderer")
    public Renderer<Role> codeRenderer() {
        return new ComponentRenderer<>(role -> {
            Span badge = new Span();
            String code = role.getCode();
            badge.setText(code != null ? code : "");

            if (code != null) {
                switch (code.toUpperCase()) {
                    case "ADMIN" -> badge.getElement().getThemeList().add("badge error");
                    case "MANAGER" -> badge.getElement().getThemeList().add("badge warning");
                    case "LIBRARIAN" -> badge.getElement().getThemeList().add("badge primary");
                    case "CATALOGER" -> badge.getElement().getThemeList().add("badge");
                    case "READER" -> badge.getElement().getThemeList().add("badge success");
                    default -> badge.getElement().getThemeList().add("badge contrast");
                }
            }
            return badge;
        });
    }
}
