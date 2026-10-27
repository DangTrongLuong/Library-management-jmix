package com.company.librarymanagementsystem.view.location;

import com.company.librarymanagementsystem.entity.Location;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "locations/:id", layout = MainView.class)
@ViewController(id = "Location.detail")
@ViewDescriptor(path = "location-detail-view.xml")
@EditedEntityContainer("locationDc")
@DialogMode(width = "42em")
public class LocationDetailView extends StandardDetailView<Location> {

    @ViewComponent
    private Select<String> statusField;

    @Autowired
    private ToastService toastService;

    @Subscribe
    public void onInit(final InitEvent event) {
        statusField.setItems(List.of("ACTIVE", "FULL", "MAINTENANCE", "INACTIVE"));
        statusField.setItemLabelGenerator(status -> {
            if (status == null) {
                return "";
            }
            return switch (status.toUpperCase()) {
                case "ACTIVE" -> "Đang sử dụng (ACTIVE)";
                case "FULL" -> "Đã đầy (FULL)";
                case "MAINTENANCE" -> "Đang bảo trì (MAINTENANCE)";
                case "INACTIVE" -> "Ngưng dùng (INACTIVE)";
                default -> status;
            };
        });
    }

    @Subscribe
    public void onInitEntity(final InitEntityEvent<Location> event) {
        Location entity = event.getEntity();
        entity.setFloor(1);
        entity.setStatus("ACTIVE");
        entity.setShelfLevel(1);
        entity.setMaxCapacity(100);
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        toastService.success("Lưu thông tin vị trí kệ thành công!");
    }
}
