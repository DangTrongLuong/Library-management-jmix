package com.company.librarymanagementsystem.view.publisher;

import com.company.librarymanagementsystem.entity.Publisher;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "publishers/:id", layout = MainView.class)
@ViewController(id = "Publisher.detail")
@ViewDescriptor(path = "publisher-detail-view.xml")
@EditedEntityContainer("publisherDc")
@DialogMode(width = "45em")
public class PublisherDetailView extends StandardDetailView<Publisher> {

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

    @Autowired
    private io.jmix.core.DataManager dataManager;

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        Publisher publisher = getEditedEntity();
        if (publisher.getName() == null || publisher.getName().trim().isEmpty()) {
            event.getErrors().add("Tên nhà xuất bản không được để trống!");
            return;
        }

        Long dupCount = dataManager.loadValue(
                "select count(p) from Publisher p where lower(trim(p.name)) = lower(trim(:name)) " +
                        "and (:id is null or p.id <> :id)", Long.class)
                .parameter("name", publisher.getName())
                .parameter("id", publisher.getId())
                .one();
        if (dupCount != null && dupCount > 0) {
            event.getErrors().add(String.format("Tên nhà xuất bản '%s' đã tồn tại trong hệ thống!", publisher.getName()));
        }

        if (publisher.getPhone() != null && !publisher.getPhone().trim().isEmpty()) {
            String cleanPhone = publisher.getPhone().trim();
            if (!cleanPhone.matches("^[0-9+\\-\\s.()]{8,20}$")) {
                event.getErrors().add("Số điện thoại không đúng định dạng!");
            }
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        newEntity = entityStates.isNew(getEditedEntity());
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        if (newEntity) {
            toastService.success(String.format("Thêm mới nhà xuất bản '%s' thành công!", getEditedEntity().getName()));
            newEntity = false;
        } else {
            toastService.success(String.format("Cập nhật nhà xuất bản '%s' thành công!", getEditedEntity().getName()));
        }
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        nameField.setInvalid(false);
        nameField.setErrorMessage(null);
    }
}
