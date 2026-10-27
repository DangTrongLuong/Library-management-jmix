package com.company.librarymanagementsystem.view.manager;

import com.company.librarymanagementsystem.entity.AuditLog;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.download.ByteArrayDownloadDataProvider;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Route(value = "audit-logs", layout = MainView.class)
@ViewController("AuditLogListView")
@ViewDescriptor("audit-log-list-view.xml")
public class AuditLogListView extends StandardView {

    @ViewComponent
    private CollectionContainer<AuditLog> auditLogsDc;
    @ViewComponent
    private CollectionLoader<AuditLog> auditLogsDl;
    @ViewComponent
    private DataGrid<AuditLog> auditLogsDataGrid;

    @ViewComponent
    private TextField searchField;
    @ViewComponent
    private Select<String> actionFilter;
    @ViewComponent
    private Span recordCountSpan;

    @ViewComponent
    private Button refreshBtn;
    @ViewComponent
    private Button exportBtn;

    @Autowired
    private ToastService toastService;
    @Autowired
    private Downloader downloader;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Subscribe
    public void onInit(final InitEvent event) {
        initFilters();
        initRenderers();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        refreshBtn.addClickListener(e -> loadData());
        exportBtn.addClickListener(e -> exportCsv());
        loadData();
    }

    private void initFilters() {
        actionFilter.setItems(Arrays.asList(
                "ALL",
                "WAIVE_FINE",
                "UNBLOCK_MEMBER",
                "CANCEL_INCORRECT_LOAN",
                "RESTORE_LOST_BOOK",
                "UPDATE_LIBRARY_SETTINGS",
                "LOAN_CREATE",
                "CREATE_USER"));

        actionFilter.setItemLabelGenerator(action -> {
            if (action == null || "ALL".equalsIgnoreCase(action)) {
                return "Tất cả loại hành động";
            }
            return switch (action) {
                case "WAIVE_FINE", "FINE_WAIVE" -> "Phê duyệt miễn/giảm phạt";
                case "UNBLOCK_MEMBER", "MEMBER_ACTIVATE" -> "Mở khóa thẻ độc giả";
                case "CANCEL_INCORRECT_LOAN", "LOAN_CANCEL" -> "Hủy phiếu mượn sai sót";
                case "RESTORE_LOST_BOOK", "BOOK_RESTORE" -> "Phục hồi sách mất tìm lại";
                case "UPDATE_LIBRARY_SETTINGS", "SETTING_UPDATE" -> "Cập nhật tham số hệ thống";
                case "LOAN_CREATE" -> "Lập phiếu mượn sách";
                case "CREATE_USER", "UPDATE_USER" -> "Quản lý nhân sự";
                default -> action;
            };
        });
        actionFilter.setValue("ALL");

        actionFilter.addValueChangeListener(e -> loadData());
        searchField.addValueChangeListener(e -> loadData());
    }

    private void initRenderers() {
        // Formatted timestamp
        auditLogsDataGrid.getColumnByKey("createdAt").setRenderer(new ComponentRenderer<>(log -> {
            String txt = log.getCreatedAt() != null ? log.getCreatedAt().format(dateFormatter) : "-";
            Span span = new Span(txt);
            span.addClassNames("text-secondary", "text-s");
            return span;
        }));

        // Action badge
        auditLogsDataGrid.getColumnByKey("actionBadge").setRenderer(new ComponentRenderer<>(log -> {
            Span badge = new Span();
            String act = log.getAction() != null ? log.getAction() : "";
            switch (act) {
                case "WAIVE_FINE", "FINE_WAIVE" -> {
                    badge.setText("Duyệt miễn phạt");
                    badge.addClassNames("badge", "warning");
                }
                case "UNBLOCK_MEMBER", "MEMBER_ACTIVATE" -> {
                    badge.setText("Mở khóa độc giả");
                    badge.addClassNames("badge", "success");
                }
                case "CANCEL_INCORRECT_LOAN", "LOAN_CANCEL" -> {
                    badge.setText("Hủy phiếu sai");
                    badge.addClassNames("badge", "error");
                }
                case "RESTORE_LOST_BOOK", "BOOK_RESTORE" -> {
                    badge.setText("Phục hồi sách");
                    badge.addClassNames("badge", "info");
                }
                case "UPDATE_LIBRARY_SETTINGS", "SETTING_UPDATE" -> {
                    badge.setText("Đổi tham số");
                    badge.addClassNames("badge", "contrast");
                }
                case "LOAN_CREATE" -> {
                    badge.setText("Lập phiếu mượn");
                    badge.addClassNames("badge", "primary");
                }
                case "LOAN_RETURN" -> {
                    badge.setText("Trả sách");
                    badge.addClassNames("badge", "success");
                }
                case "CREATE_USER", "UPDATE_USER" -> {
                    badge.setText("Nhân sự (" + act + ")");
                    badge.addClassNames("badge", "primary");
                }
                default -> {
                    badge.setText(act);
                    badge.addClassNames("badge");
                }
            }
            return badge;
        }));

        // Click on details to view full dialog
        auditLogsDataGrid.getColumnByKey("details").setRenderer(new ComponentRenderer<>(log -> {
            Div container = new Div();
            container.addClassNames("truncate", "cursor-pointer");
            container.setWidthFull();
            Span text = new Span(log.getDetails() != null ? log.getDetails() : "-");
            text.addClassNames("text-s");
            container.add(text);
            container.addClickListener(e -> showDetailDialog(log));
            return container;
        }));
    }

    private void loadData() {
        String filter = actionFilter.getValue();
        String kw = searchField.getValue() != null ? searchField.getValue().trim() : "";

        StringBuilder q = new StringBuilder("select e from AuditLog e where 1=1 ");

        boolean hasCustomFilter = false;
        if (filter != null && !"ALL".equalsIgnoreCase(filter)) {
            switch (filter) {
                case "WAIVE_FINE" -> {
                    q.append("and e.action in ('WAIVE_FINE', 'FINE_WAIVE') ");
                    hasCustomFilter = true;
                }
                case "UNBLOCK_MEMBER" -> {
                    q.append("and e.action in ('UNBLOCK_MEMBER', 'MEMBER_ACTIVATE') ");
                    hasCustomFilter = true;
                }
                case "CANCEL_INCORRECT_LOAN" -> {
                    q.append("and e.action in ('CANCEL_INCORRECT_LOAN', 'LOAN_CANCEL') ");
                    hasCustomFilter = true;
                }
                case "RESTORE_LOST_BOOK" -> {
                    q.append("and e.action in ('RESTORE_LOST_BOOK', 'BOOK_RESTORE') ");
                    hasCustomFilter = true;
                }
                case "UPDATE_LIBRARY_SETTINGS" -> {
                    q.append("and e.action in ('UPDATE_LIBRARY_SETTINGS', 'SETTING_UPDATE') ");
                    hasCustomFilter = true;
                }
                case "CREATE_USER" -> {
                    q.append("and e.action in ('CREATE_USER', 'UPDATE_USER') ");
                    hasCustomFilter = true;
                }
                default -> q.append("and e.action = :act ");
            }
        }

        if (!kw.isEmpty()) {
            q.append("and (lower(e.username) like lower(concat('%', :kw, '%')) ")
                    .append("or lower(e.entityName) like lower(concat('%', :kw, '%')) ")
                    .append("or lower(e.entityId) like lower(concat('%', :kw, '%')) ")
                    .append("or lower(e.details) like lower(concat('%', :kw, '%'))) ");
        }

        q.append("order by e.createdAt desc");

        auditLogsDl.setQuery(q.toString());

        if (filter != null && !"ALL".equalsIgnoreCase(filter) && !hasCustomFilter) {
            auditLogsDl.setParameter("act", filter);
        } else {
            auditLogsDl.removeParameter("act");
        }

        if (!kw.isEmpty()) {
            auditLogsDl.setParameter("kw", kw);
        } else {
            auditLogsDl.removeParameter("kw");
        }

        auditLogsDl.load();

        int count = auditLogsDc.getItems().size();
        recordCountSpan.setText(String.format("Tổng số bản ghi: %d", count));
    }

    private void showDetailDialog(AuditLog log) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Chi tiết bản ghi kiểm toán #" + log.getId());
        dialog.setWidth("520px");

        Div content = new Div();
        content.addClassNames("flex", "flex-col", "gap-s", "p-s", "bg-contrast-5", "rounded-m", "text-s");

        content.add(new Span(
                "Thời gian: " + (log.getCreatedAt() != null ? log.getCreatedAt().format(dateFormatter) : "-")));
        content.add(new Span("Tài khoản thực hiện: " + log.getUsername()));
        content.add(new Span("Hành động: " + log.getAction()));
        content.add(new Span("Đối tượng: " + log.getEntityName() + " (ID / Mã: " + log.getEntityId() + ")"));

        Span detailsHeader = new Span("Nội dung chi tiết / Căn cứ quyết định:");
        detailsHeader.addClassNames("font-bold", "mt-s");
        content.add(detailsHeader);

        Div detailsBox = new Div();
        detailsBox.addClassNames("p-s", "bg-base", "rounded-s", "border", "border-contrast-10", "whitespace-pre-wrap");
        detailsBox.setText(log.getDetails() != null ? log.getDetails() : "(Không có chi tiết)");
        content.add(detailsBox);

        Button closeBtn = new Button("Đóng", e -> dialog.close());

        dialog.add(content);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private void exportCsv() {
        List<AuditLog> items = auditLogsDc.getItems();
        if (items.isEmpty()) {
            toastService.error("Không có dữ liệu để xuất!");
            return;
        }

        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM for Excel Vietnamese compatibility
        csv.append("\uFEFF");
        csv.append("ID,Thời Gian,Tài Khoản,Hành Động,Đối Tượng,Mã Đối Tượng,Chi Tiết\n");

        for (AuditLog l : items) {
            String time = l.getCreatedAt() != null ? l.getCreatedAt().format(dateFormatter) : "";
            String user = l.getUsername() != null ? l.getUsername() : "";
            String act = l.getAction() != null ? l.getAction() : "";
            String ent = l.getEntityName() != null ? l.getEntityName() : "";
            String eid = l.getEntityId() != null ? l.getEntityId() : "";
            String det = l.getDetails() != null ? l.getDetails().replace("\"", "\"\"") : "";

            csv.append(String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    l.getId(), time, user, act, ent, eid, det));
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        downloader.download(new ByteArrayDownloadDataProvider(bytes, 8192, "UTF-8"), "audit-logs.csv");
        toastService.success("Đã xuất " + items.size() + " bản ghi kiểm toán sang file CSV!");
    }
}
