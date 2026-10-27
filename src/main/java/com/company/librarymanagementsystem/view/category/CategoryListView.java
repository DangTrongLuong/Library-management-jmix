package com.company.librarymanagementsystem.view.category;

import com.company.librarymanagementsystem.entity.Category;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "categories", layout = MainView.class)
@ViewController(id = "Category.list")
@ViewDescriptor(path = "category-list-view.xml")
@LookupComponent("categoriesDataGrid")
@DialogMode(width = "64em")
public class CategoryListView extends StandardListView<Category> {

    @ViewComponent
    private CollectionContainer<Category> categoriesDc;

    @ViewComponent
    private CollectionLoader<Category> categoriesDl;

    @ViewComponent
    private DataGrid<Category> categoriesDataGrid;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private com.vaadin.flow.component.button.Button createButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button editButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button removeButton;

    @Autowired
    private ToastService toastService;

    @Autowired
    private com.company.librarymanagementsystem.security.AccessControlService accessControlService;

    @Subscribe
    public void onInit(final InitEvent event) {
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.CATEGORY_MANAGE);
        accessControlService.applyPermission(categoriesDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.CATEGORY_MANAGE);

        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.CATEGORY_MANAGE);
        accessControlService.applyPermission(categoriesDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.CATEGORY_MANAGE);

        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.CATEGORY_MANAGE);
        accessControlService.applyPermission(categoriesDataGrid.getAction("removeAction"), com.company.librarymanagementsystem.security.Permissions.CATEGORY_MANAGE);
    }

    @Subscribe("searchField")
    public void onSearchFieldValueChange(final ComponentValueChangeEvent<TextField, String> event) {
        String filter = event.getValue();
        if (filter != null && !filter.trim().isEmpty()) {
            categoriesDl.setQuery("select e from Category e where lower(e.name) like concat('%', lower(:search), '%') " +
                    "or lower(e.slug) like concat('%', lower(:search), '%') " +
                    "or lower(e.ddcCode) like concat('%', lower(:search), '%') " +
                    "order by e.name");
            categoriesDl.setParameter("search", filter.trim());
        } else {
            categoriesDl.setQuery("select e from Category e order by e.name");
            categoriesDl.removeParameter("search");
        }
        categoriesDl.load();
    }

    @Autowired
    private io.jmix.core.DataManager dataManager;

    @Install(to = "categoriesDataGrid.removeAction", subject = "beforeActionPerformedHandler")
    public void categoriesDataGridRemoveBeforeActionPerformed(final RemoveOperation.BeforeActionPerformedEvent<Category> event) {
        for (Category category : event.getItems()) {
            Long childCount = dataManager.loadValue(
                    "select count(c) from Category c where c.parent = :category", Long.class)
                    .parameter("category", category)
                    .one();
            if (childCount != null && childCount > 0) {
                event.preventAction();
                toastService.error(String.format(
                        "Không thể xóa thể loại '%s' vì đang có %d thể loại con trực thuộc!",
                        category.getName(), childCount));
                return;
            }

            Long bookCount = dataManager.loadValue(
                    "select count(b) from Book b join b.categories c where c = :category", Long.class)
                    .parameter("category", category)
                    .one();
            if (bookCount != null && bookCount > 0) {
                event.preventAction();
                toastService.error(String.format(
                        "Không thể xóa thể loại '%s' vì đang có %d đầu sách thuộc thể loại này!",
                        category.getName(), bookCount));
                return;
            }
        }
    }

    @Install(to = "categoriesDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void categoriesDataGridRemoveAfterActionPerformed(final RemoveOperation.AfterActionPerformedEvent<Category> event) {
        toastService.success("Xóa thể loại thành công!");
    }

    @Supply(to = "categoriesDataGrid.name", subject = "renderer")
    public Renderer<Category> nameRenderer() {
        return new ComponentRenderer<>(cat -> {
            Span nameSpan = new Span(cat.getName());
            nameSpan.getStyle().set("font-weight", "600");
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            return nameSpan;
        });
    }

    @Supply(to = "categoriesDataGrid.parent", subject = "renderer")
    public Renderer<Category> parentRenderer() {
        return new ComponentRenderer<>(cat -> {
            if (cat.getParent() != null) {
                Span badge = new Span(cat.getParent().getName());
                badge.getElement().getThemeList().add("badge");
                return badge;
            }
            Span rootBadge = new Span("Danh mục gốc");
            rootBadge.getElement().getThemeList().add("badge contrast");
            return rootBadge;
        });
    }

    @Supply(to = "categoriesDataGrid.description", subject = "renderer")
    public Renderer<Category> descriptionRenderer() {
        return new ComponentRenderer<>(cat -> {
            String desc = cat.getDescription();
            if (desc != null && !desc.trim().isEmpty()) {
                Span span = new Span(desc);
                span.getStyle().set("overflow", "hidden");
                span.getStyle().set("text-overflow", "ellipsis");
                span.getStyle().set("white-space", "nowrap");
                span.getStyle().set("display", "block");
                span.getStyle().set("max-width", "28em");
                span.setTitle(desc);
                return span;
            }
            Span empty = new Span("—");
            empty.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            return empty;
        });
    }
}
