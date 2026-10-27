package com.company.librarymanagementsystem.view.member;

import com.company.librarymanagementsystem.entity.MemberType;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "member-types/:id", layout = MainView.class)
@ViewController(id = "MemberType.detail")
@ViewDescriptor(path = "member-type-detail-view.xml")
@EditedEntityContainer("memberTypeDc")
@DialogMode(width = "46em")
public class MemberTypeDetailView extends StandardDetailView<MemberType> {

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
        if (getEditedEntity().getCode() != null) {
            getEditedEntity().setCode(getEditedEntity().getCode().trim().toUpperCase());
        }
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        if (newEntity) {
            toastService.success(String.format("Thêm mới loại độc giả '%s' thành công!", getEditedEntity().getName()));
            newEntity = false;
        } else {
            toastService.success(String.format("Cập nhật loại độc giả '%s' thành công!", getEditedEntity().getName()));
        }
    }
}
