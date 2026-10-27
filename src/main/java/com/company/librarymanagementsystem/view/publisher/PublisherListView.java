package com.company.librarymanagementsystem.view.publisher;

import com.company.librarymanagementsystem.entity.Publisher;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.html.Anchor;
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

@Route(value = "publishers", layout = MainView.class)
@ViewController(id = "Publisher.list")
@ViewDescriptor(path = "publisher-list-view.xml")
@LookupComponent("publishersDataGrid")
@DialogMode(width = "64em")
public class PublisherListView extends StandardListView<Publisher> {

    @ViewComponent
    private CollectionContainer<Publisher> publishersDc;

    @ViewComponent
    private CollectionLoader<Publisher> publishersDl;

    @ViewComponent
    private DataGrid<Publisher> publishersDataGrid;

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
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.PUBLISHER_MANAGE);
        accessControlService.applyPermission(publishersDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.PUBLISHER_MANAGE);

        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.PUBLISHER_MANAGE);
        accessControlService.applyPermission(publishersDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.PUBLISHER_MANAGE);

        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.PUBLISHER_MANAGE);
        accessControlService.applyPermission(publishersDataGrid.getAction("removeAction"), com.company.librarymanagementsystem.security.Permissions.PUBLISHER_MANAGE);
    }

    @Subscribe("searchField")
    public void onSearchFieldValueChange(final ComponentValueChangeEvent<TextField, String> event) {
        String filter = event.getValue();
        if (filter != null && !filter.trim().isEmpty()) {
            publishersDl.setQuery("select e from Publisher e where lower(e.name) like concat('%', lower(:search), '%') " +
                    "or lower(e.address) like concat('%', lower(:search), '%') " +
                    "or lower(e.phone) like concat('%', lower(:search), '%') " +
                    "or lower(e.email) like concat('%', lower(:search), '%') " +
                    "or lower(e.website) like concat('%', lower(:search), '%') " +
                    "order by e.name");
            publishersDl.setParameter("search", filter.trim());
        } else {
            publishersDl.setQuery("select e from Publisher e order by e.name");
            publishersDl.removeParameter("search");
        }
        publishersDl.load();
    }

    @Autowired
    private io.jmix.core.DataManager dataManager;

    @Install(to = "publishersDataGrid.removeAction", subject = "beforeActionPerformedHandler")
    public void publishersDataGridRemoveBeforeActionPerformed(final RemoveOperation.BeforeActionPerformedEvent<Publisher> event) {
        for (Publisher publisher : event.getItems()) {
            Long bookCount = dataManager.loadValue(
                    "select count(b) from Book b where b.publisher = :publisher", Long.class)
                    .parameter("publisher", publisher)
                    .one();
            if (bookCount != null && bookCount > 0) {
                event.preventAction();
                toastService.error(String.format(
                        "Không thể xóa nhà xuất bản '%s' vì đang có %d đầu sách trực thuộc!",
                        publisher.getName(), bookCount));
                return;
            }
        }
    }

    @Install(to = "publishersDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void publishersDataGridRemoveAfterActionPerformed(final RemoveOperation.AfterActionPerformedEvent<Publisher> event) {
        toastService.success("Xóa nhà xuất bản thành công!");
    }

    @Supply(to = "publishersDataGrid.name", subject = "renderer")
    public Renderer<Publisher> nameRenderer() {
        return new ComponentRenderer<>(publisher -> {
            Span nameSpan = new Span(publisher.getName());
            nameSpan.getStyle().set("font-weight", "600");
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            return nameSpan;
        });
    }

    @Supply(to = "publishersDataGrid.address", subject = "renderer")
    public Renderer<Publisher> addressRenderer() {
        return new ComponentRenderer<>(publisher -> {
            String address = publisher.getAddress();
            if (address != null && !address.trim().isEmpty()) {
                Span span = new Span(address);
                span.getStyle().set("overflow", "hidden");
                span.getStyle().set("text-overflow", "ellipsis");
                span.getStyle().set("white-space", "nowrap");
                span.getStyle().set("display", "block");
                span.getStyle().set("max-width", "28em");
                span.setTitle(address);
                return span;
            }
            Span empty = new Span("—");
            empty.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            return empty;
        });
    }
}
