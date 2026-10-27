package com.company.librarymanagementsystem.view.manager;

import com.company.librarymanagementsystem.entity.LibrarySetting;
import com.company.librarymanagementsystem.service.AuditLogService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.*;

@Route(value = "library-settings", layout = MainView.class)
@ViewController("LibrarySettingView")
@ViewDescriptor("library-setting-view.xml")
public class LibrarySettingView extends StandardView {

    @ViewComponent
    private Div settingsContainer;
    @ViewComponent
    private Button refreshBtn;
    @ViewComponent
    private Button saveAllBtn;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private ToastService toastService;
    @Autowired
    private AuditLogService auditLogService;

    private final Map<String, TextField> inputFieldsMap = new LinkedHashMap<>();

    @Subscribe
    public void onReady(final ReadyEvent event) {
        refreshBtn.addClickListener(e -> loadSettings());
        saveAllBtn.addClickListener(e -> onSaveAll());
        loadSettings();
    }

    private void loadSettings() {
        settingsContainer.removeAll();
        inputFieldsMap.clear();

        List<LibrarySetting> settings = dataManager.load(LibrarySetting.class)
                .query("select s from LibrarySetting s order by s.category, s.key")
                .list();

        Map<String, List<LibrarySetting>> categorized = new LinkedHashMap<>();
        for (LibrarySetting s : settings) {
            categorized.computeIfAbsent(s.getCategory() != null ? s.getCategory() : "GENERAL", k -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<String, List<LibrarySetting>> entry : categorized.entrySet()) {
            Div sectionCard = new Div();
            sectionCard.addClassNames("bg-base", "p-m", "rounded-l", "border", "border-contrast-10", "shadow-xs");

            String catTitle = switch (entry.getKey()) {
                case "LOAN_RESTRICTION" -> "Ràng Buộc & Chặn Điều Kiện Mượn Sách";
                case "FINE" -> "Quy Tắc Tính Phạt & Bồi Thường Mất/Hỏng Sách";
                case "RESERVATION" -> "Chính Sách Giữ Chỗ Sách Đặt Trước";
                case "NOTIFICATION" -> "Cấu Hình Gửi Thông Báo & Nhắc Hạn";
                default -> "Tham Số Khác";
            };

            H4 secHeader = new H4(catTitle);
            secHeader.addClassNames("m-0", "mb-m", "text-m", "font-bold", "text-primary");
            sectionCard.add(secHeader);

            VerticalLayout formLayout = new VerticalLayout();
            formLayout.setPadding(false);
            formLayout.setSpacing(true);

            for (LibrarySetting s : entry.getValue()) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
                row.addClassNames("p-s", "border-b", "border-contrast-5");
                row.getStyle().set("gap", "16px");

                VerticalLayout labelCol = new VerticalLayout();
                labelCol.setPadding(false);
                labelCol.setSpacing(false);
                labelCol.getStyle().set("flex-grow", "1");

                Span nameSpan = new Span(s.getName() != null ? s.getName() : s.getKey());
                nameSpan.getStyle().set("font-weight", "600").set("font-size", "14px");

                Span descSpan = new Span(s.getDescription() != null ? s.getDescription() : s.getKey());
                descSpan.addClassNames("text-secondary");
                descSpan.getStyle().set("font-size", "12px");

                labelCol.add(nameSpan, descSpan);

                TextField valField = new TextField();
                valField.setValue(s.getValue() != null ? s.getValue() : "");
                valField.setWidth("220px");
                valField.getStyle().set("text-align", "right");

                inputFieldsMap.put(s.getKey(), valField);

                row.add(labelCol, valField);
                formLayout.add(row);
            }

            sectionCard.add(formLayout);
            settingsContainer.add(sectionCard);
        }
    }

    private void onSaveAll() {
        int updatedCount = 0;
        for (Map.Entry<String, TextField> entry : inputFieldsMap.entrySet()) {
            String key = entry.getKey();
            String newVal = entry.getValue().getValue();

            Optional<LibrarySetting> opt = dataManager.load(LibrarySetting.class)
                    .query("select s from LibrarySetting s where s.key = :key")
                    .parameter("key", key)
                    .optional();

            if (opt.isPresent()) {
                LibrarySetting s = opt.get();
                if (!Objects.equals(s.getValue(), newVal)) {
                    String oldVal = s.getValue();
                    s.setValue(newVal);
                    s.setUpdatedAt(OffsetDateTime.now());
                    dataManager.save(s);
                    updatedCount++;

                    auditLogService.log("SETTING_UPDATE", "LibrarySetting", key,
                            String.format("Cập nhật tham số %s từ '%s' sang '%s'", key, oldVal, newVal));
                }
            }
        }

        if (updatedCount > 0) {
            toastService.success(String.format("Đã cập nhật thành công %d tham số cấu hình thư viện!", updatedCount));
        } else {
            toastService.info("Không có thay đổi nào cần lưu!");
        }
    }
}
