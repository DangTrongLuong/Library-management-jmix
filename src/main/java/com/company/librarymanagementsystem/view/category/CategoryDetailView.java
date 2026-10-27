package com.company.librarymanagementsystem.view.category;

import com.company.librarymanagementsystem.entity.Category;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Route(value = "categories/:id", layout = MainView.class)
@ViewController(id = "Category.detail")
@ViewDescriptor(path = "category-detail-view.xml")
@EditedEntityContainer("categoryDc")
@DialogMode(width = "45em")
public class CategoryDetailView extends StandardDetailView<Category> {

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
        Category category = getEditedEntity();
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            event.getErrors().add("Tên thể loại không được để trống!");
            return;
        }

        String slug = category.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = toSlug(category.getName());
            category.setSlug(slug);
        }

        Long dupSlug = dataManager.loadValue(
                "select count(c) from Category c where c.slug = :slug and (:id is null or c.id <> :id)", Long.class)
                .parameter("slug", slug)
                .parameter("id", category.getId())
                .one();
        if (dupSlug != null && dupSlug > 0) {
            event.getErrors().add(String.format("Mã slug '%s' đã tồn tại cho một thể loại khác!", slug));
        }

        Category parent = category.getParent();
        if (parent != null && category.getId() != null) {
            if (category.getId().equals(parent.getId())) {
                event.getErrors().add("Thể loại cha không thể là chính thể loại này!");
                return;
            }

            Category current = parent;
            int depth = 0;
            while (current != null && depth < 20) {
                depth++;
                if (current.getId() != null && current.getId().equals(category.getId())) {
                    event.getErrors().add("Phát hiện vòng lặp phân cấp: Thể loại cha được chọn đang là con hoặc cháu của thể loại này!");
                    return;
                }
                if (current.getParent() != null) {
                    current = dataManager.load(Category.class)
                            .id(current.getParent().getId())
                            .fetchPlan(builder -> builder.add("parent"))
                            .optional()
                            .orElse(null);
                } else {
                    current = null;
                }
            }
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        newEntity = entityStates.isNew(getEditedEntity());
        Category category = getEditedEntity();
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            category.setSlug(toSlug(category.getName()));
        }
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        if (newEntity) {
            toastService.success(String.format("Thêm mới thể loại '%s' thành công!", getEditedEntity().getName()));
            newEntity = false;
        } else {
            toastService.success(String.format("Cập nhật thể loại '%s' thành công!", getEditedEntity().getName()));
        }
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        nameField.setInvalid(false);
        nameField.setErrorMessage(null);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("[\\s]").matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9-]", "").replaceAll("-+", "-");
        return slug;
    }
}
