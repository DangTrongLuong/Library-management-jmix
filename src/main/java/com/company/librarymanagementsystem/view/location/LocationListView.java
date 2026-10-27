package com.company.librarymanagementsystem.view.location;

import com.company.librarymanagementsystem.entity.Location;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "locations", layout = MainView.class)
@ViewController(id = "Location.list")
@ViewDescriptor(path = "location-list-view.xml")
@LookupComponent("locationsDataGrid")
@DialogMode(width = "50em")
public class LocationListView extends StandardListView<Location> {

    @ViewComponent
    private CollectionContainer<Location> locationsDc;

    @ViewComponent
    private CollectionLoader<Location> locationsDl;

    @ViewComponent
    private DataGrid<Location> locationsDataGrid;

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
    private DataManager dataManager;

    @Autowired
    private com.company.librarymanagementsystem.security.AccessControlService accessControlService;

    @Subscribe
    public void onInit(final InitEvent event) {
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.LOCATION_MANAGE);
        accessControlService.applyPermission(locationsDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.LOCATION_MANAGE);

        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.LOCATION_MANAGE);
        accessControlService.applyPermission(locationsDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.LOCATION_MANAGE);

        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.LOCATION_MANAGE);
        accessControlService.applyPermission(locationsDataGrid.getAction("removeAction"), com.company.librarymanagementsystem.security.Permissions.LOCATION_MANAGE);
    }

    @Subscribe("searchField")
    public void onSearchFieldValueChange(final ComponentValueChangeEvent<TextField, String> event) {
        String filter = event.getValue();
        if (filter != null && !filter.trim().isEmpty()) {
            locationsDl.setQuery("select e from Location e where e.deletedAt is null and (" +
                    "lower(e.code) like concat('%', lower(:search), '%') " +
                    "or lower(e.name) like concat('%', lower(:search), '%') " +
                    "or lower(e.roomOrZone) like concat('%', lower(:search), '%') " +
                    "or lower(e.aisle) like concat('%', lower(:search), '%') " +
                    "or lower(e.description) like concat('%', lower(:search), '%')) " +
                    "order by e.floor, e.code");
            locationsDl.setParameter("search", filter.trim());
        } else {
            locationsDl.setQuery("select e from Location e where e.deletedAt is null order by e.floor, e.code");
            locationsDl.removeParameter("search");
        }
        locationsDl.load();
    }

    @Install(to = "locationsDataGrid.removeAction", subject = "beforeActionPerformedHandler")
    public void locationsDataGridRemoveBeforeActionPerformed(final RemoveOperation.BeforeActionPerformedEvent<Location> event) {
        for (Location location : event.getItems()) {
            Long copyCount = dataManager.loadValue(
                            "select count(c) from BookCopy c where c.location = :location and c.deletedAt is null", Long.class)
                    .parameter("location", location)
                    .one();
            if (copyCount != null && copyCount > 0) {
                event.preventAction();
                toastService.error(String.format(
                        "Không thể xóa vị trí kệ '%s' vì đang chứa %d bản sao sách!",
                        location.getCode() != null ? location.getCode() : "", copyCount));
                return;
            }
        }
    }

    @Install(to = "locationsDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void locationsDataGridRemoveAfterActionPerformed(final RemoveOperation.AfterActionPerformedEvent<Location> event) {
        toastService.success("Xóa vị trí kệ thành công!");
    }

    @Supply(to = "locationsDataGrid.code", subject = "renderer")
    public Renderer<Location> codeRenderer() {
        return new ComponentRenderer<>(loc -> {
            String c = (loc != null && loc.getCode() != null) ? loc.getCode() : "-";
            Span span = new Span(c);
            span.getElement().getThemeList().add("badge");
            span.getStyle().set("font-weight", "bold");
            span.getStyle().set("letter-spacing", "0.5px");
            return span;
        });
    }

    @Supply(to = "locationsDataGrid.status", subject = "renderer")
    public Renderer<Location> statusRenderer() {
        return new ComponentRenderer<>(loc -> {
            String s = (loc != null && loc.getStatus() != null) ? loc.getStatus() : "ACTIVE";
            Span span = new Span();
            span.getElement().getThemeList().add("badge");
            switch (s.toUpperCase()) {
                case "ACTIVE" -> {
                    span.setText("Đang sử dụng");
                    span.getElement().getThemeList().add("success");
                }
                case "FULL" -> {
                    span.setText("Đã đầy");
                    span.getElement().getThemeList().add("error");
                }
                case "MAINTENANCE" -> {
                    span.setText("Bảo trì");
                    span.getElement().getThemeList().add("warning");
                }
                case "INACTIVE" -> {
                    span.setText("Ngưng dùng");
                    span.getElement().getThemeList().add("contrast");
                }
                default -> span.setText(s);
            }
            return span;
        });
    }

    @Supply(to = "locationsDataGrid.maxCapacity", subject = "renderer")
    public Renderer<Location> maxCapacityRenderer() {
        return new ComponentRenderer<>(loc -> {
            if (loc == null || loc.getMaxCapacity() == null) {
                return new Span("-");
            }
            Span span = new Span(loc.getMaxCapacity() + " cuốn");
            span.getElement().getThemeList().add("badge small contrast");
            return span;
        });
    }

    @Supply(to = "locationsDataGrid.floor", subject = "renderer")
    public Renderer<Location> floorRenderer() {
        return new ComponentRenderer<>(loc -> {
            if (loc == null || loc.getFloor() == null) {
                return new Span("-");
            }
            Span span = new Span(loc.getFloor() == 0 ? "Tầng Hầm" : "Tầng " + loc.getFloor());
            span.getElement().getThemeList().add("badge contrast small");
            return span;
        });
    }
}
