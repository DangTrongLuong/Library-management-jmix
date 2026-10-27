package com.company.librarymanagementsystem.view.author;

import com.company.librarymanagementsystem.entity.Author;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "authors/:id", layout = MainView.class)
@ViewController(id = "Author.detail")
@ViewDescriptor(path = "author-detail-view.xml")
@EditedEntityContainer("authorDc")
@DialogMode(width = "45em")
public class AuthorDetailView extends StandardDetailView<Author> {

    @ViewComponent
    private TextField fullNameField;

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
        Author author = getEditedEntity();
        if (author.getFullName() == null || author.getFullName().trim().isEmpty()) {
            event.getErrors().add("Họ và tên tác giả không được để trống!");
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        newEntity = entityStates.isNew(getEditedEntity());
        Author author = getEditedEntity();
        if (newEntity && author.getFullName() != null) {
            Long dupCount = dataManager.loadValue(
                    "select count(a) from Author a where lower(trim(a.fullName)) = lower(trim(:name))", Long.class)
                    .parameter("name", author.getFullName())
                    .one();
            if (dupCount != null && dupCount > 0) {
                toastService.warning(String.format("Lưu ý: Hệ thống đã có tác giả mang tên '%s'.", author.getFullName()));
            }
        }
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        if (newEntity) {
            toastService.success(String.format("Thêm mới tác giả '%s' thành công!", getEditedEntity().getFullName()));
            newEntity = false;
        } else {
            toastService.success(String.format("Cập nhật tác giả '%s' thành công!", getEditedEntity().getFullName()));
        }
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        fullNameField.setInvalid(false);
        fullNameField.setErrorMessage(null);
    }
}
