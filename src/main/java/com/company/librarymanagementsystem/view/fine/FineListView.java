package com.company.librarymanagementsystem.view.fine;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.security.AccessControlService;
import com.company.librarymanagementsystem.security.Permissions;
import com.company.librarymanagementsystem.service.FineService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Route(value = "fines", layout = MainView.class)
@ViewController("Fine.list")
@ViewDescriptor("fine-list-view.xml")
@LookupComponent("finesDataGrid")
@DialogMode(width = "85em")
public class FineListView extends StandardView {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @ViewComponent
    private Div kpiContainer;

    @ViewComponent
    private TextField searchField;
    @ViewComponent
    private Select<String> statusFilterSelect;
    @ViewComponent
    private Select<String> typeFilterSelect;
    @ViewComponent
    private CollectionLoader<Fine> finesDl;
    @ViewComponent
    private DataGrid<Fine> finesDataGrid;

    @ViewComponent
    private Button payFineBtn;
    @ViewComponent
    private Button payAllBtn;
    @ViewComponent
    private Button waiveFineBtn;
    @ViewComponent
    private Button reportLostOrDamagedBtn;
    @ViewComponent
    private Button receiptHistoryBtn;
    @ViewComponent
    private Button refreshBtn;

    @Autowired
    private FineService fineService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private AccessControlService accessControlService;

    @Subscribe
    public void onInit(InitEvent event) {
        initFilters();
        initGridRenderers();
        initActionButtons();
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        accessControlService.applyPermission(payFineBtn, Permissions.FINE_PAYMENT);
        accessControlService.applyPermission(payAllBtn, Permissions.FINE_PAYMENT);
        accessControlService.applyPermission(waiveFineBtn, Permissions.FINE_WAIVE);
        accessControlService.applyPermission(reportLostOrDamagedBtn, Permissions.FINE_CREATE);

        loadData();
    }

    private void initFilters() {
        statusFilterSelect.setItems(Arrays.asList("", "UNPAID", "PARTIAL", "PAID", "WAIVED"));
        statusFilterSelect.setItemLabelGenerator(s -> switch (s) {
            case "UNPAID" -> "Chưa thanh toán";
            case "PARTIAL" -> "Đã trả một phần";
            case "PAID" -> "Đã thanh toán đủ";
            case "WAIVED" -> "Đã miễn giảm";
            default -> "Tất cả trạng thái";
        });
        statusFilterSelect.setValue("");

        typeFilterSelect.setItems(Arrays.asList("", "OVERDUE", "LOST", "DAMAGED", "CARD_FEE", "OTHER"));
        typeFilterSelect.setItemLabelGenerator(t -> switch (t) {
            case "OVERDUE" -> "Quá hạn (OVERDUE)";
            case "LOST" -> "Mất sách (LOST)";
            case "DAMAGED" -> "Hỏng sách (DAMAGED)";
            case "CARD_FEE" -> "Phí làm thẻ (CARD_FEE)";
            case "OTHER" -> "Phí khác (OTHER)";
            default -> "Tất cả loại phạt";
        });
        typeFilterSelect.setValue("");
    }

    private void initGridRenderers() {
        // Loại phạt
        finesDataGrid.getColumnByKey("fineTypeCol").setRenderer(new ComponentRenderer<>(fine -> {
            Span badge = new Span();
            badge.getStyle()
                    .set("padding", "3px 8px")
                    .set("border-radius", "4px")
                    .set("font-weight", "600")
                    .set("font-size", "12px");

            String type = fine.getFineType();
            if (FineType.OVERDUE.getId().equalsIgnoreCase(type)) {
                badge.setText(fine.getDaysOverdue() != null ? "Trễ " + fine.getDaysOverdue() + " ngày" : "Quá hạn");
                badge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
            } else if (FineType.LOST.getId().equalsIgnoreCase(type)) {
                badge.setText("Mất sách");
                badge.getStyle().set("background", "#ffe4e6").set("color", "#be123c");
            } else if (FineType.DAMAGED.getId().equalsIgnoreCase(type)) {
                badge.setText("Hỏng sách");
                badge.getStyle().set("background", "#fef9c3").set("color", "#854d0e");
            } else if (FineType.CARD_FEE.getId().equalsIgnoreCase(type)) {
                badge.setText("Phí làm thẻ");
                badge.getStyle().set("background", "#eff6ff").set("color", "#1d4ed8");
            } else {
                badge.setText(type);
                badge.getStyle().set("background", "#f1f5f9").set("color", "#475569");
            }
            return createCenteredCell(badge);
        }));

        // Sách liên quan
        finesDataGrid.getColumnByKey("bookInfoCol").setRenderer(new ComponentRenderer<>(fine -> {
            if (fine.getLoanItem() != null && fine.getLoanItem().getBookCopy() != null) {
                BookCopy copy = fine.getLoanItem().getBookCopy();
                String title = copy.getBook() != null ? copy.getBook().getTitle() : "";
                Span span = new Span(String.format("%s [%s]", title, copy.getBarcode()));
                span.getStyle().set("font-size", "13px");
                return span;
            }
            return new Span("-");
        }));

        // Số tiền phạt
        finesDataGrid.getColumnByKey("amountCol").setRenderer(new ComponentRenderer<>(fine -> {
            Span span = new Span(
                    String.format("%,.0f VNĐ", fine.getAmount() != null ? fine.getAmount() : BigDecimal.ZERO));
            span.getStyle().set("font-weight", "600");
            return span;
        }));

        // Đã thu
        finesDataGrid.getColumnByKey("paidAmountCol").setRenderer(new ComponentRenderer<>(fine -> {
            BigDecimal paid = fine.getPaidAmount() != null ? fine.getPaidAmount() : BigDecimal.ZERO;
            Span span = new Span(String.format("%,.0f VNĐ", paid));
            if (paid.compareTo(BigDecimal.ZERO) > 0) {
                span.getStyle().set("color", "#16a34a").set("font-weight", "500");
            } else {
                span.getStyle().set("color", "#94a3b8");
            }
            return span;
        }));

        // Còn nợ
        finesDataGrid.getColumnByKey("remainingCol").setRenderer(new ComponentRenderer<>(fine -> {
            BigDecimal rem = fine.getRemainingAmount();
            Span span = new Span(String.format("%,.0f VNĐ", rem));
            span.getStyle().set("font-weight", "700");
            if (rem.compareTo(BigDecimal.ZERO) > 0 && !FineStatus.WAIVED.getId().equalsIgnoreCase(fine.getStatus())) {
                span.getStyle().set("color", "#dc2626");
            } else {
                span.getStyle().set("color", "#16a34a");
            }
            return span;
        }));

        // Trạng thái
        finesDataGrid.getColumnByKey("statusCol").setRenderer(new ComponentRenderer<>(fine -> {
            Span badge = new Span();
            badge.getStyle()
                    .set("padding", "3px 8px")
                    .set("border-radius", "4px")
                    .set("font-weight", "600")
                    .set("font-size", "12px");

            String st = fine.getStatus();
            if (FineStatus.UNPAID.getId().equalsIgnoreCase(st)) {
                badge.setText("CHƯA THANH TOÁN");
                badge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c").set("border",
                        "1px solid #fca5a5");
            } else if (FineStatus.PARTIAL.getId().equalsIgnoreCase(st)) {
                badge.setText("TRẢ MỘT PHẦN");
                badge.getStyle().set("background", "#fef3c7").set("color", "#b45309").set("border",
                        "1px solid #fde68a");
            } else if (FineStatus.PAID.getId().equalsIgnoreCase(st)) {
                badge.setText("ĐÃ TẤT TOÁN");
                badge.getStyle().set("background", "#dcfce7").set("color", "#15803d").set("border",
                        "1px solid #86efac");
            } else if (FineStatus.WAIVED.getId().equalsIgnoreCase(st)) {
                badge.setText("ĐÃ MIỄN GIẢM");
                badge.getStyle().set("background", "#f1f5f9").set("color", "#64748b");
            } else {
                badge.setText(st);
            }
            return createCenteredCell(badge);
        }));

        // Ngày ghi nhận
        finesDataGrid.getColumnByKey("createdAtCol").setRenderer(new ComponentRenderer<>(fine -> {
            if (fine.getCreatedAt() != null) {
                return createCenteredCell(new Span(fine.getCreatedAt().format(dtf)));
            }
            return createCenteredCell(new Span("-"));
        }));
    }

    private void initActionButtons() {
        payFineBtn.addClickListener(this::onPayFineClick);
        payAllBtn.addClickListener(this::onPayAllClick);
        waiveFineBtn.addClickListener(this::onWaiveFineClick);
        reportLostOrDamagedBtn.addClickListener(this::onReportLostOrDamagedClick);
        receiptHistoryBtn.addClickListener(this::onReceiptHistoryClick);
        refreshBtn.addClickListener(e -> loadData());
    }

    private void loadData() {
        String status = statusFilterSelect.getValue();
        String type = typeFilterSelect.getValue();
        String search = searchField.getValue();

        finesDl.setParameter("statusFilter", (status == null || status.isBlank()) ? null : status);
        finesDl.setParameter("fineTypeFilter", (type == null || type.isBlank()) ? null : type);
        finesDl.setParameter("searchText", (search == null || search.isBlank()) ? null : search.trim());
        finesDl.load();

        updateStats();
    }

    @Subscribe("searchBtn")
    public void onSearchBtnClick(ClickEvent<Button> event) {
        finesDl.setFirstResult(0);
        loadData();
    }

    @Subscribe("clearFilterBtn")
    public void onClearFilterBtnClick(ClickEvent<Button> event) {
        searchField.clear();
        statusFilterSelect.setValue("");
        typeFilterSelect.setValue("");
        finesDl.setFirstResult(0);
        loadData();
    }

    private void updateStats() {
        if (kpiContainer == null) return;
        kpiContainer.removeAll();

        FineService.FineStats stats = fineService.getFineStats();

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        boolean hasOutstanding = stats.unpaidFinesCount() > 0;
        grid.add(createKpiCard(
                "Tổng Nợ Tồn Đọng",
                String.format("%,.0f VNĐ", stats.totalOutstanding()),
                String.format("%d khoản phạt chưa thu", stats.unpaidFinesCount()),
                hasOutstanding ? "bg-error-10 text-error border-error-50" : "bg-base border-contrast-10",
                hasOutstanding ? "text-error font-bold" : "text-primary"
        ));

        boolean hasCollected = stats.collectedToday().compareTo(BigDecimal.ZERO) > 0;
        grid.add(createKpiCard(
                "Đã Thu Hôm Nay",
                String.format("%,.0f VNĐ", stats.collectedToday()),
                String.format("%d giao dịch thanh toán", stats.collectedTodayCount()),
                hasCollected ? "bg-success-10 text-success border-success-30" : "bg-base border-contrast-10",
                hasCollected ? "text-success font-bold" : "text-primary"
        ));

        boolean hasIndebted = stats.indebtedMembersCount() > 0;
        grid.add(createKpiCard(
                "Độc Giả Nợ Phạt",
                String.format("%d độc giả", stats.indebtedMembersCount()),
                "Đang bị treo tư cách mượn",
                hasIndebted ? "bg-warning-10 text-warning border-warning-20" : "bg-base border-contrast-10",
                hasIndebted ? "text-warning font-bold" : "text-primary"
        ));

        grid.add(createKpiCard(
                "Đã Miễn / Giảm",
                String.format("%,.0f VNĐ", stats.totalWaived()),
                String.format("%d khoản đã duyệt miễn", stats.waivedCount()),
                "bg-base border-contrast-10",
                "text-primary"
        ));

        kpiContainer.add(grid);
    }

    private Div createKpiCard(String label, String value, String subtext, String boxClass, String valueClass) {
        Div card = new Div();
        card.addClassNames("bg-base", "rounded-m", "border", "p-s", "shadow-xs", "flex", "flex-col", "justify-between", "box-border");
        card.getStyle().set("box-sizing", "border-box").set("min-width", "0");
        if (boxClass.contains("border-")) {
            for (String c : boxClass.split(" ")) {
                if (!c.isBlank()) card.addClassName(c);
            }
        } else {
            card.addClassNames("border-contrast-10");
        }

        Span lbl = new Span(label);
        lbl.addClassNames("text-secondary", "font-medium", "uppercase");
        lbl.getStyle().set("font-size", "10.5px");
        lbl.getStyle().set("letter-spacing", "0.5px");

        Span val = new Span(value);
        val.addClassNames("font-bold", "my-xs");
        val.getStyle().set("font-size", "18px");
        for (String c : valueClass.split(" ")) {
            if (!c.isBlank()) val.addClassName(c);
        }

        Span sub = new Span(subtext);
        sub.addClassNames("text-secondary");
        sub.getStyle().set("font-size", "11px");

        card.add(lbl, val, sub);
        return card;
    }

    /**
     * Dialog Thu tiền phạt cho 1 khoản phạt cụ thể (L09)
     */
    private void onPayFineClick(ClickEvent<Button> event) {
        Fine selected = finesDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 khoản phạt trên bảng để thu tiền!");
            return;
        }

        if (FineStatus.PAID.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.info("Khoản phạt này đã được thanh toán đủ!");
            return;
        }
        if (FineStatus.WAIVED.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Khoản phạt này đã được miễn giảm!");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Thu Tiền Phạt Tại Quầy");
        dialog.setWidth("36em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        String memberInfo = selected.getMember() != null
                ? String.format("[%s] %s", selected.getMember().getMemberCode(), selected.getMember().getFullName())
                : "-";
        BigDecimal remaining = selected.getRemainingAmount();

        Div infoBox = new Div();
        infoBox.getStyle()
                .set("background", "#f8fafc")
                .set("padding", "12px")
                .set("border-radius", "6px")
                .set("border", "1px solid #e2e8f0")
                .set("font-size", "13px");
        Span remSpan = new Span(String.format("• SỐ TIỀN CÒN NỢ: %,.0f VNĐ", remaining));
        remSpan.getStyle().set("font-weight", "700").set("color", "#dc2626");

        infoBox.add(
                new Div(new Span("• Độc giả: " + memberInfo)),
                new Div(new Span("• Khoản phạt: " + selected.getFineType() + " - "
                        + (selected.getDescription() != null ? selected.getDescription() : ""))),
                new Div(new Span(String.format("• Tổng tiền phạt: %,.0f VNĐ | Đã trả: %,.0f VNĐ", selected.getAmount(),
                        selected.getPaidAmount()))),
                new Div(remSpan));

        BigDecimalField amountField = new BigDecimalField("Số tiền thu (VNĐ)");
        amountField.setWidthFull();
        amountField.setValue(remaining);
        amountField.setHelperText(String.format("Có thể thu toàn bộ hoặc một phần (Tối đa: %,.0f VNĐ)", remaining));

        Select<PaymentMethod> methodSelect = new Select<>();
        methodSelect.setLabel("Phương thức thanh toán");
        methodSelect.setWidthFull();
        methodSelect.setItems(PaymentMethod.values());
        methodSelect.setItemLabelGenerator(PaymentMethod::getCaption);
        methodSelect.setValue(PaymentMethod.CASH);

        TextField refField = new TextField("Mã giao dịch / Tham chiếu");
        refField.setWidthFull();
        refField.setPlaceholder("Nhập mã giao dịch nếu chuyển khoản / POS / Ví...");
        refField.setVisible(false);

        methodSelect.addValueChangeListener(e -> {
            boolean isCash = e.getValue() == PaymentMethod.CASH;
            refField.setVisible(!isCash);
        });

        TextField noteField = new TextField("Ghi chú thu tiền");
        noteField.setWidthFull();
        noteField.setPlaceholder("Ghi chú nếu có...");

        layout.add(infoBox, amountField, methodSelect, refField, noteField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận thu tiền", e -> {
            BigDecimal amt = amountField.getValue();
            if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                toastService.warning("Vui lòng nhập số tiền hợp lệ (> 0 VNĐ)!");
                return;
            }
            if (amt.compareTo(remaining) > 0) {
                toastService.warning(
                        String.format("Số tiền thu không được vượt quá số nợ còn lại (%,.0f VNĐ)!", remaining));
                return;
            }

            try {
                Payment p = fineService.payFine(selected.getId(), amt, methodSelect.getValue(), refField.getValue(),
                        noteField.getValue());
                toastService
                        .success(String.format("Đã thu %,.0f VNĐ thành công! Số biên lai: %s", amt, p.getReceiptNo()));
                dialog.close();
                loadData();
            } catch (Exception ex) {
                toastService.error("Lỗi khi thu tiền: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button cancelBtn = new Button("Đóng", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    /**
     * Dialog Thu toàn bộ nợ của một độc giả (Tiện ích quầy)
     */
    private void onPayAllClick(ClickEvent<Button> event) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Thu Toàn Bộ Nợ Của Độc Giả");
        dialog.setWidth("38em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        ComboBox<Member> memberCombo = new ComboBox<>("Chọn độc giả cần tất toán nợ");
        memberCombo.setWidthFull();
        memberCombo.setPlaceholder("Tìm độc giả có nợ phạt...");

        List<Member> indebtedMembers = dataManager.load(Member.class)
                .query("select distinct f.member from Fine f where f.status in ('UNPAID', 'PARTIAL') order by f.member.fullName")
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("memberType", FetchPlan.BASE))
                .list();

        memberCombo.setItems(indebtedMembers);
        memberCombo.setItemLabelGenerator(m -> String.format("[%s] %s (%s)",
                m.getMemberCode(), m.getFullName(), m.getMemberType() != null ? m.getMemberType().getName() : ""));

        Div debtSummary = new Div();
        debtSummary.getStyle()
                .set("background", "#fef2f2")
                .set("padding", "12px")
                .set("border-radius", "6px")
                .set("border", "1px solid #fecaca")
                .set("font-size", "13px")
                .set("display", "none");

        Select<PaymentMethod> methodSelect = new Select<>();
        methodSelect.setLabel("Phương thức thanh toán");
        methodSelect.setWidthFull();
        methodSelect.setItems(PaymentMethod.values());
        methodSelect.setItemLabelGenerator(PaymentMethod::getCaption);
        methodSelect.setValue(PaymentMethod.CASH);

        TextField refField = new TextField("Mã giao dịch / Tham chiếu");
        refField.setWidthFull();
        refField.setPlaceholder("Nhập mã giao dịch nếu chuyển khoản / POS / Ví...");
        refField.setVisible(false);

        methodSelect.addValueChangeListener(e -> refField.setVisible(e.getValue() != PaymentMethod.CASH));

        TextField noteField = new TextField("Ghi chú");
        noteField.setWidthFull();
        noteField.setPlaceholder("Ghi chú tất toán nợ...");

        memberCombo.addValueChangeListener(e -> {
            Member m = e.getValue();
            if (m != null) {
                BigDecimal totalOutstanding = dataManager.loadValue(
                        "select coalesce(sum(f.amount - f.paidAmount), 0) from Fine f where f.member.id = :mId and f.status in ('UNPAID', 'PARTIAL')",
                        BigDecimal.class)
                        .parameter("mId", m.getId())
                        .one();
                Long countFines = dataManager.loadValue(
                        "select count(f) from Fine f where f.member.id = :mId and f.status in ('UNPAID', 'PARTIAL')",
                        Long.class)
                        .parameter("mId", m.getId())
                        .one();

                Span totalSpan = new Span(String.format("• TỔNG SỐ TIỀN CẦN THANH TOÁN: %,.0f VNĐ", totalOutstanding));
                totalSpan.getStyle().set("font-weight", "700").set("color", "#dc2626").set("font-size", "15px");

                debtSummary.removeAll();
                debtSummary.add(
                        new Div(new Span(String.format("• Tổng số khoản phạt đang nợ: %d khoản", countFines))),
                        new Div(totalSpan));
                debtSummary.getStyle().set("display", "block");
            } else {
                debtSummary.getStyle().set("display", "none");
            }
        });

        layout.add(memberCombo, debtSummary, methodSelect, refField, noteField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận tất toán toàn bộ", e -> {
            Member m = memberCombo.getValue();
            if (m == null) {
                toastService.warning("Vui lòng chọn độc giả cần tất toán nợ!");
                return;
            }

            try {
                List<Payment> payments = fineService.payAllFinesForMember(m.getId(), methodSelect.getValue(),
                        refField.getValue(), noteField.getValue());
                toastService.success(
                        String.format("Đã tất toán toàn bộ %d khoản phạt cho độc giả %s! Tư cách mượn đã được mở lại.",
                                payments.size(), m.getFullName()));
                dialog.close();
                loadData();
            } catch (Exception ex) {
                toastService.error("Lỗi khi tất toán nợ: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button cancelBtn = new Button("Đóng", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    /**
     * Dialog Miễn / giảm khoản phạt (L10, BR14)
     */
    private void onWaiveFineClick(ClickEvent<Button> event) {
        Fine selected = finesDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 khoản phạt trên bảng để miễn giảm!");
            return;
        }

        if (FineStatus.PAID.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Khoản phạt này đã thanh toán đủ, không thể miễn giảm!");
            return;
        }
        if (FineStatus.WAIVED.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.info("Khoản phạt này đã được miễn giảm trước đó!");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Miễn / Giảm Khoản Phạt");
        dialog.setWidth("34em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        String memberInfo = selected.getMember() != null ? selected.getMember().getFullName() : "";
        BigDecimal rem = selected.getRemainingAmount();

        Span info = new Span(String.format("Bạn có chắc chắn muốn miễn giảm khoản nợ %,.0f VNĐ của độc giả '%s'?",
                rem, memberInfo));
        info.getStyle().set("font-size", "14px");

        TextField reasonField = new TextField("Lý do miễn giảm (Bắt buộc)");
        reasonField.setWidthFull();
        reasonField.setRequired(true);
        reasonField.setPlaceholder("Ví dụ: Độc giả có đơn xin miễn giảm có xác nhận của khoa...");

        layout.add(info, reasonField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận Miễn giảm", e -> {
            String reason = reasonField.getValue();
            if (reason == null || reason.trim().isBlank()) {
                toastService.warning("Vui lòng nhập lý do miễn giảm (bắt buộc)!");
                return;
            }

            try {
                fineService.waiveFine(selected.getId(), reason);
                toastService.success("Đã phê duyệt miễn giảm khoản phạt thành công!");
                dialog.close();
                loadData();
            } catch (Exception ex) {
                toastService.error("Lỗi khi miễn giảm: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);

        Button cancelBtn = new Button("Hủy bỏ", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    /**
     * Dialog Báo mất / hỏng sách tại quầy (L08, BR09)
     */
    private void onReportLostOrDamagedClick(ClickEvent<Button> event) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Ghi Nhận Sách Mất / Hỏng Tại Quầy");
        dialog.setWidth("38em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        Select<String> actionTypeSelect = new Select<>();
        actionTypeSelect.setLabel("Hình thức ghi nhận");
        actionTypeSelect.setWidthFull();
        actionTypeSelect.setItems("LOST", "DAMAGED");
        actionTypeSelect.setItemLabelGenerator(s -> "LOST".equals(s)
                ? "Báo mất sách (Phạt 150% giá bìa + 20.000đ)"
                : "Báo hỏng sách (Phạt 50% giá bìa)");
        actionTypeSelect.setValue("LOST");

        ComboBox<LoanItem> loanItemCombo = new ComboBox<>("Chọn cuốn sách đang mượn");
        loanItemCombo.setWidthFull();
        loanItemCombo.setPlaceholder("Tìm theo mã thẻ, tên độc giả, mã vạch, tên sách...");

        List<LoanItem> borrowedItems = dataManager.load(LoanItem.class)
                .query("select li from LoanItem li where li.status = 'BORROWED' order by li.loan.loanDate desc")
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE))
                        .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE).add("member", FetchPlan.BASE)))
                .list();

        loanItemCombo.setItems(borrowedItems);
        if (borrowedItems.isEmpty()) {
            loanItemCombo.setHelperText("Hiện không có cuốn sách nào đang được mượn. Vui lòng lập phiếu mượn trước!");
        }
        loanItemCombo.setItemLabelGenerator(li -> {
            String mName = li.getLoan() != null && li.getLoan().getMember() != null
                    ? li.getLoan().getMember().getFullName()
                    : "";
            String mCode = li.getLoan() != null && li.getLoan().getMember() != null
                    ? li.getLoan().getMember().getMemberCode()
                    : "";
            String bTitle = li.getBookCopy() != null && li.getBookCopy().getBook() != null
                    ? li.getBookCopy().getBook().getTitle()
                    : "";
            String barcode = li.getBookCopy() != null ? li.getBookCopy().getBarcode() : "";
            return String.format("[%s] %s | %s (%s)", mCode, mName, bTitle, barcode);
        });

        BigDecimalField fineField = new BigDecimalField("Số tiền phạt bồi thường (VNĐ)");
        fineField.setWidthFull();

        TextField noteField = new TextField("Ghi chú chi tiết / Tình trạng");
        noteField.setWidthFull();
        noteField.setPlaceholder("Nhập tình trạng hỏng rách hoặc hoàn cảnh mất sách...");

        Runnable recomputeFine = () -> {
            LoanItem li = loanItemCombo.getValue();
            if (li != null && li.getBookCopy() != null && li.getBookCopy().getBook() != null) {
                BigDecimal price = li.getBookCopy().getBook().getPrice() != null
                        ? li.getBookCopy().getBook().getPrice()
                        : BigDecimal.valueOf(100000);
                if ("LOST".equals(actionTypeSelect.getValue())) {
                    BigDecimal lostFine = price.multiply(BigDecimal.valueOf(1.5)).add(BigDecimal.valueOf(20000));
                    fineField.setValue(lostFine);
                    fineField.setHelperText(String.format("Giá bìa: %,.0f VNĐ × 1.5 + 20.000đ phí xử lý", price));
                } else {
                    BigDecimal damFine = price.multiply(BigDecimal.valueOf(0.5));
                    fineField.setValue(damFine);
                    fineField.setHelperText(String.format("Giá bìa: %,.0f VNĐ × 50%% bồi thường hư hại", price));
                }
            } else {
                fineField.setValue(null);
                fineField.setHelperText("");
            }
        };

        loanItemCombo.addValueChangeListener(e -> recomputeFine.run());
        actionTypeSelect.addValueChangeListener(e -> recomputeFine.run());

        layout.add(actionTypeSelect, loanItemCombo, fineField, noteField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận ghi nhận & Tạo phạt", e -> {
            LoanItem li = loanItemCombo.getValue();
            if (li == null) {
                toastService.warning("Vui lòng chọn cuốn sách đang mượn!");
                return;
            }

            try {
                if ("LOST".equals(actionTypeSelect.getValue())) {
                    Fine f = fineService.reportLostBook(li.getId(), fineField.getValue(), noteField.getValue());
                    toastService.success(String.format(
                            "Đã ghi nhận báo mất sách! Đã tạo khoản phạt bồi thường %,.0f VNĐ.", f.getAmount()));
                } else {
                    Fine f = fineService.reportDamagedBook(li.getId(), fineField.getValue(), noteField.getValue());
                    toastService.success(String.format("Đã ghi nhận sách hỏng! Đã tạo khoản phạt bồi thường %,.0f VNĐ.",
                            f.getAmount()));
                }
                dialog.close();
                loadData();
            } catch (Exception ex) {
                toastService.error("Lỗi: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Đóng", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    /**
     * Dialog Xem lịch sử các biên lai thanh toán gần đây (L09)
     */
    private void onReceiptHistoryClick(ClickEvent<Button> event) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Lịch Sử Biên Lai Thu Tiền Phạt (Payments)");
        dialog.setWidth("55em");

        List<Payment> payments = dataManager.load(Payment.class)
                .query("select p from Payment p order by p.paidAt desc")
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("member", FetchPlan.BASE)
                        .add("fine", FetchPlan.BASE)
                        .add("receivedBy", FetchPlan.BASE))
                .maxResults(50)
                .list();

        Grid<Payment> grid = new Grid<>();
        grid.setWidthFull();
        grid.setHeight("25em");
        grid.setItems(payments);

        grid.addColumn(Payment::getReceiptNo).setHeader("Số biên lai").setAutoWidth(true);
        grid.addColumn(p -> p.getMember() != null ? p.getMember().getFullName() : "").setHeader("Độc giả")
                .setAutoWidth(true);
        grid.addColumn(p -> String.format("%,.0f VNĐ", p.getAmount())).setHeader("Số tiền")
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END).setAutoWidth(true);
        grid.addColumn(p -> {
            PaymentMethod pm = PaymentMethod.fromId(p.getMethod());
            return pm != null ? pm.getCaption() : p.getMethod();
        }).setHeader("Phương thức").setAutoWidth(true);
        grid.addColumn(p -> p.getPaidAt() != null ? p.getPaidAt().format(dtf) : "").setHeader("Thời điểm thu")
                .setAutoWidth(true);
        grid.addColumn(p -> p.getReceivedBy() != null ? p.getReceivedBy().getUsername() : "-").setHeader("Người thu")
                .setAutoWidth(true);
        grid.addColumn(Payment::getNote).setHeader("Ghi chú").setAutoWidth(true);

        VerticalLayout layout = new VerticalLayout(grid);
        layout.setPadding(false);
        dialog.add(layout);

        Button closeBtn = new Button("Đóng", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private HorizontalLayout createCenteredCell(Span span) {
        HorizontalLayout hl = new HorizontalLayout(span);
        hl.setWidthFull();
        hl.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        hl.setAlignItems(FlexComponent.Alignment.CENTER);
        hl.setPadding(false);
        hl.setSpacing(false);
        return hl;
    }
}
