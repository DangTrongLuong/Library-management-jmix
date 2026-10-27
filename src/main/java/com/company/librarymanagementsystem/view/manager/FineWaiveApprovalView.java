package com.company.librarymanagementsystem.view.manager;

import com.company.librarymanagementsystem.entity.Fine;
import com.company.librarymanagementsystem.entity.FineStatus;
import com.company.librarymanagementsystem.service.AuditLogService;
import com.company.librarymanagementsystem.service.FineService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Route(value = "fine-waive-approvals", layout = MainView.class)
@ViewController("FineWaiveApprovalView")
@ViewDescriptor("fine-waive-approval-view.xml")
public class FineWaiveApprovalView extends StandardView {

    @ViewComponent
    private CollectionContainer<Fine> finesDc;
    @ViewComponent
    private CollectionLoader<Fine> finesDl;
    @ViewComponent
    private DataGrid<Fine> finesDataGrid;

    @ViewComponent
    private TextField searchField;
    @ViewComponent
    private Select<String> statusFilter;
    @ViewComponent
    private Span summaryBadge;

    @ViewComponent
    private Button refreshBtn;
    @ViewComponent
    private Button waiveActionBtn;

    @Autowired
    private FineService fineService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private AuditLogService auditLogService;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Subscribe
    public void onInit(final InitEvent event) {
        initFilters();
        initRenderers();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        refreshBtn.addClickListener(e -> loadData());
        waiveActionBtn.addClickListener(e -> onWaiveActionClick());
        loadData();
    }

    private void initFilters() {
        statusFilter.setItems(Arrays.asList("ALL", "UNPAID_ONLY", "WAIVED_ONLY"));
        statusFilter.setItemLabelGenerator(s -> {
            if (s == null || "ALL".equalsIgnoreCase(s)) {
                return "Tất cả trạng thái";
            }
            return switch (s) {
                case "UNPAID_ONLY" -> "Đang nợ (Chưa miễn)";
                case "WAIVED_ONLY" -> "Đã duyệt miễn giảm";
                default -> s;
            };
        });
        statusFilter.setValue("UNPAID_ONLY");

        statusFilter.addValueChangeListener(e -> loadData());
        searchField.addValueChangeListener(e -> loadData());
    }

    private void loadData() {
        String filterType = statusFilter.getValue();
        String kw = searchField.getValue() != null ? searchField.getValue().trim() : "";

        StringBuilder q = new StringBuilder("select f from Fine f where 1=1 ");

        if ("UNPAID_ONLY".equals(filterType)) {
            q.append("and f.status in ('UNPAID', 'PARTIAL') ");
        } else if ("WAIVED_ONLY".equals(filterType)) {
            q.append("and f.status = 'WAIVED' ");
        }

        if (!kw.isBlank()) {
            q.append("and (lower(f.member.fullName) like concat('%', lower(:kw), '%') ")
             .append("or lower(f.member.memberCode) like concat('%', lower(:kw), '%') ")
             .append("or lower(f.description) like concat('%', lower(:kw), '%')) ");
        }

        q.append("order by f.createdAt desc");

        finesDl.setQuery(q.toString());
        if (!kw.isBlank()) {
            finesDl.setParameter("kw", kw);
        } else {
            finesDl.removeParameter("kw");
        }
        finesDl.load();

        updateSummaryBadge();
    }

    private void updateSummaryBadge() {
        int total = finesDc.getItems().size();
        summaryBadge.setText(String.format("Tổng: %d bản ghi", total));
    }

    private void initRenderers() {
        finesDataGrid.getColumnByKey("memberCol").setRenderer(new ComponentRenderer<>(fine -> {
            VerticalLayout box = new VerticalLayout();
            box.setPadding(false);
            box.setSpacing(false);
            if (fine.getMember() != null) {
                Span name = new Span(fine.getMember().getFullName());
                name.getStyle().set("font-weight", "600");
                Span code = new Span("[" + fine.getMember().getMemberCode() + "]");
                code.addClassNames("badge contrast small");
                box.add(name, code);
            } else {
                box.add(new Span("-"));
            }
            return box;
        }));

        finesDataGrid.getColumnByKey("typeCol").setRenderer(new ComponentRenderer<>(fine -> {
            Span sp = new Span(fine.getFineType() != null ? fine.getFineType() : "-");
            sp.addClassNames("badge primary small");
            return sp;
        }));

        finesDataGrid.getColumnByKey("amountCol").setRenderer(new ComponentRenderer<>(fine -> {
            Span sp = new Span(currencyFormat.format(fine.getAmount()) + " đ");
            sp.getStyle().set("font-weight", "600");
            return sp;
        }));

        finesDataGrid.getColumnByKey("paidCol").setRenderer(new ComponentRenderer<>(fine -> {
            return new Span(currencyFormat.format(fine.getPaidAmount()) + " đ");
        }));

        finesDataGrid.getColumnByKey("remainingCol").setRenderer(new ComponentRenderer<>(fine -> {
            BigDecimal rem = fine.getRemainingAmount();
            Span sp = new Span(currencyFormat.format(rem) + " đ");
            sp.getStyle().set("font-weight", "700").set("color", "#dc2626");
            return sp;
        }));

        finesDataGrid.getColumnByKey("statusCol").setRenderer(new ComponentRenderer<>(fine -> {
            Span sp = new Span();
            String st = fine.getStatus();
            if (FineStatus.WAIVED.getId().equalsIgnoreCase(st)) {
                sp.setText("ĐÃ MIỄN GIẢM");
                sp.addClassNames("badge success small font-bold");
            } else if (FineStatus.PAID.getId().equalsIgnoreCase(st)) {
                sp.setText("ĐÃ THANH TOÁN");
                sp.addClassNames("badge primary small");
            } else {
                sp.setText("CHƯA THANH TOÁN");
                sp.addClassNames("badge error small font-bold");
            }
            return sp;
        }));

        finesDataGrid.getColumnByKey("waiveInfoCol").setRenderer(new ComponentRenderer<>(fine -> {
            if (fine.getWaivedReason() != null && !fine.getWaivedReason().isBlank()) {
                VerticalLayout v = new VerticalLayout();
                v.setPadding(false);
                v.setSpacing(false);
                Span reason = new Span("Lý do: " + fine.getWaivedReason());
                reason.getStyle().set("font-size", "11.5px");
                String by = fine.getWaivedBy() != null ? fine.getWaivedBy().getUsername() : "Manager";
                Span approver = new Span("Duyệt bởi: " + by);
                approver.addClassNames("text-secondary");
                approver.getStyle().set("font-size", "11px");
                v.add(reason, approver);
                return v;
            }
            return new Span("—");
        }));
    }

    private void onWaiveActionClick() {
        Fine selected = finesDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 khoản phạt trên bảng để duyệt miễn giảm!");
            return;
        }

        if (FineStatus.WAIVED.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.info("Khoản phạt này đã được phê duyệt miễn giảm trước đó rồi!");
            return;
        }
        if (FineStatus.PAID.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Khoản phạt này đã thanh toán xong, không thể miễn giảm!");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Xác Nhận Phê Duyệt Miễn / Giảm Phạt");
        dialog.setWidth("36em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        String memberInfo = selected.getMember() != null ? selected.getMember().getFullName() : "Độc giả";
        BigDecimal rem = selected.getRemainingAmount();

        Div infoBox = new Div();
        infoBox.addClassNames("p-m", "bg-contrast-5", "rounded-m", "border", "border-contrast-10", "flex", "flex-col", "gap-xs");
        infoBox.add(
                new Span("• Độc giả: " + memberInfo),
                new Span("• Khoản phạt: " + selected.getFineType() + " - " + (selected.getDescription() != null ? selected.getDescription() : "")),
                new Span("• Số tiền còn nợ được miễn: " + currencyFormat.format(rem) + " VNĐ")
        );

        TextField reasonField = new TextField("Lý do miễn giảm (Bắt buộc)");
        reasonField.setWidthFull();
        reasonField.setRequired(true);
        reasonField.setPlaceholder("Nhập căn cứ/đơn đề xuất của độc giả hoặc quyết định của thư viện...");

        layout.add(infoBox, reasonField);
        dialog.add(layout);

        Button confirmBtn = new Button("Phê Duyệt Miễn Phạt", e -> {
            String reason = reasonField.getValue();
            if (reason == null || reason.trim().isBlank()) {
                toastService.warning("Vui lòng nhập lý do miễn giảm (bắt buộc)!");
                return;
            }

            try {
                fineService.waiveFine(selected.getId(), reason.trim());
                auditLogService.log("FINE_WAIVE", "Fine", String.valueOf(selected.getId()),
                        String.format("Duyệt miễn giảm nợ phạt %,.0f VNĐ cho độc giả %s. Lý do: %s",
                                rem, memberInfo, reason.trim()));

                toastService.success(String.format("Đã phê duyệt miễn giảm nợ phạt thành công cho độc giả %s!", memberInfo));
                dialog.close();
                loadData();
            } catch (Exception ex) {
                toastService.error("Lỗi khi miễn giảm phạt: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button cancelBtn = new Button("Đóng", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }
}
