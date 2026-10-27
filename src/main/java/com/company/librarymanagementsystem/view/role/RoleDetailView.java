package com.company.librarymanagementsystem.view.role;

import com.company.librarymanagementsystem.entity.Role;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "roles/:id", layout = MainView.class)
@ViewController(id = "Role.detail")
@ViewDescriptor(path = "role-detail-view.xml")
@EditedEntityContainer("roleDc")
public class RoleDetailView extends StandardDetailView<Role> {

    @ViewComponent
    private TextField codeField;

    @ViewComponent
    private TextField nameField;

    @Autowired
    private EntityStates entityStates;

    @Autowired
    private ToastService toastService;

    private boolean newEntity;

    @Subscribe
    public void onInit(final InitEvent event) {
        setShowSaveNotification(false);
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        newEntity = entityStates.isNew(getEditedEntity());
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        if (newEntity) {
            toastService.success(String.format("Tạo mới vai trò '%s' thành công!", getEditedEntity().getName()));
            newEntity = false;
        } else {
            toastService.success(String.format("Cập nhật thông tin vai trò '%s' thành công!", getEditedEntity().getName()));
        }
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        codeField.setInvalid(false);
        codeField.setErrorMessage(null);
        nameField.setInvalid(false);
        nameField.setErrorMessage(null);
    }
}
