package com.company.librarymanagementsystem.view.author;

import com.company.librarymanagementsystem.entity.Author;
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

@Route(value = "authors", layout = MainView.class)
@ViewController(id = "Author.list")
@ViewDescriptor(path = "author-list-view.xml")
@LookupComponent("authorsDataGrid")
@DialogMode(width = "64em")
public class AuthorListView extends StandardListView<Author> {

    @ViewComponent
    private CollectionContainer<Author> authorsDc;

    @ViewComponent
    private CollectionLoader<Author> authorsDl;

    @ViewComponent
    private DataGrid<Author> authorsDataGrid;

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
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.AUTHOR_MANAGE);
        accessControlService.applyPermission(authorsDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.AUTHOR_MANAGE);

        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.AUTHOR_MANAGE);
        accessControlService.applyPermission(authorsDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.AUTHOR_MANAGE);

        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.AUTHOR_MANAGE);
        accessControlService.applyPermission(authorsDataGrid.getAction("removeAction"), com.company.librarymanagementsystem.security.Permissions.AUTHOR_MANAGE);
    }

    @Subscribe("searchField")
    public void onSearchFieldValueChange(final ComponentValueChangeEvent<TextField, String> event) {
        String filter = event.getValue();
        if (filter != null && !filter.trim().isEmpty()) {
            authorsDl.setQuery("select e from Author e where lower(e.fullName) like concat('%', lower(:search), '%') " +
                    "or lower(e.nationality) like concat('%', lower(:search), '%') " +
                    "order by e.fullName");
            authorsDl.setParameter("search", filter.trim());
        } else {
            authorsDl.setQuery("select e from Author e order by e.fullName");
            authorsDl.removeParameter("search");
        }
        authorsDl.load();
    }

    @Autowired
    private io.jmix.core.DataManager dataManager;

    @Install(to = "authorsDataGrid.removeAction", subject = "beforeActionPerformedHandler")
    public void authorsDataGridRemoveBeforeActionPerformed(final RemoveOperation.BeforeActionPerformedEvent<Author> event) {
        for (Author author : event.getItems()) {
            Long bookCount = dataManager.loadValue(
                    "select count(b) from Book b join b.authors a where a = :author", Long.class)
                    .parameter("author", author)
                    .one();
            if (bookCount != null && bookCount > 0) {
                event.preventAction();
                toastService.error(String.format(
                        "Không thể xóa tác giả '%s' vì đang có %d đầu sách được liên kết với tác giả này!",
                        author.getFullName(), bookCount));
                return;
            }
        }
    }

    @Install(to = "authorsDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void authorsDataGridRemoveAfterActionPerformed(final RemoveOperation.AfterActionPerformedEvent<Author> event) {
        toastService.success("Xóa tác giả thành công!");
    }

    @Supply(to = "authorsDataGrid.fullName", subject = "renderer")
    public Renderer<Author> fullNameRenderer() {
        return new ComponentRenderer<>(author -> {
            Span nameSpan = new Span(author.getFullName());
            nameSpan.getStyle().set("font-weight", "600");
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            return nameSpan;
        });
    }

    @Supply(to = "authorsDataGrid.biography", subject = "renderer")
    public Renderer<Author> biographyRenderer() {
        return new ComponentRenderer<>(author -> {
            String bio = author.getBiography();
            if (bio != null && !bio.trim().isEmpty()) {
                Span span = new Span(bio);
                span.getStyle().set("overflow", "hidden");
                span.getStyle().set("text-overflow", "ellipsis");
                span.getStyle().set("white-space", "nowrap");
                span.getStyle().set("display", "block");
                span.getStyle().set("max-width", "28em");
                span.setTitle(bio);
                return span;
            }
            Span empty = new Span("—");
            empty.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            return empty;
        });
    }
}
