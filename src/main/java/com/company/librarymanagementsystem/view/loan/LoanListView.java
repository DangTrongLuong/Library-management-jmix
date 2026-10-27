package com.company.librarymanagementsystem.view.loan;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.LoanService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Route(value = "loans", layout = MainView.class)
@ViewController("Loan.list")
@ViewDescriptor("loan-list-view.xml")
@LookupComponent("loanItemsDataGrid")
@DialogMode(width = "64em")
public class LoanListView extends StandardView {

    @ViewComponent
    private CollectionContainer<LoanItem> loanItemsDc;
    @ViewComponent
    private CollectionLoader<LoanItem> loanItemsDl;
    @ViewComponent
    private DataGrid<LoanItem> loanItemsDataGrid;

    @ViewComponent
    private TextField searchField;
    @ViewComponent
    private Select<String> statusFilterSelect;
    @ViewComponent
    private Button searchBtn;
    @ViewComponent
    private Button clearFilterBtn;
    @ViewComponent
    private Button createLoanBtn;
    @ViewComponent
    private Button viewDetailBtn;
    @ViewComponent
    private Button returnBookBtn;
    @ViewComponent
    private Button renewBtn;
    @ViewComponent
    private Button refreshBtn;

    @ViewComponent
    private Div kpiContainer;

    @Autowired
    private LoanService loanService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Subscribe
    public void onInit(InitEvent event) {
        initFilters();
        initRenderers();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        loadData();
    }

    private void initFilters() {
        statusFilterSelect.setItems(List.of("Tất cả", "Đang mượn", "Quá hạn", "Đã trả", "Đã mất"));
        statusFilterSelect.setValue("Tất cả");

        searchBtn.addClickListener(e -> loadData());
        clearFilterBtn.addClickListener(e -> {
            searchField.clear();
            statusFilterSelect.setValue("Tất cả");
            loadData();
        });
        refreshBtn.addClickListener(e -> loadData());

        searchField.addKeyDownListener(com.vaadin.flow.component.Key.ENTER, e -> loadData());

        createLoanBtn.addClickListener(this::onCreateLoanClick);
        viewDetailBtn.addClickListener(this::onViewDetailClick);
        returnBookBtn.addClickListener(this::onReturnBookClick);
        renewBtn.addClickListener(this::onRenewClick);

        loanItemsDataGrid.addItemDoubleClickListener(e -> openLoanDetail(e.getItem()));
    }

    private Div createCenteredCell(Component component) {
        Div div = new Div(component);
        div.setWidthFull();
        div.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("text-align", "center");
        return div;
    }

    private void setColumnAlignCenter(String key) {
        com.vaadin.flow.component.grid.Grid.Column<LoanItem> col = loanItemsDataGrid.getColumnByKey(key);
        if (col != null) {
            col.setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
        }
    }

    private void initRenderers() {
        setColumnAlignCenter("bookCopy.barcode");
        setColumnAlignCenter("loan.loanDate");
        setColumnAlignCenter("dueDate");
        setColumnAlignCenter("overdueStatus");
        setColumnAlignCenter("renewCount");
        setColumnAlignCenter("itemStatus");
        setColumnAlignCenter("returnCondition");
        setColumnAlignCenter("returnedAt");
        setColumnAlignCenter("receivedBy.username");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Mã vạch căn giữa
        loanItemsDataGrid.getColumnByKey("bookCopy.barcode").setRenderer(new ComponentRenderer<>(item -> {
            String bc = (item.getBookCopy() != null && item.getBookCopy().getBarcode() != null)
                    ? item.getBookCopy().getBarcode() : "-";
            Span span = new Span(bc);
            span.getStyle().set("font-family", "monospace").set("font-weight", "500");
            return createCenteredCell(span);
        }));

        // Hạn trả căn giữa
        loanItemsDataGrid.getColumnByKey("dueDate").setRenderer(new ComponentRenderer<>(item -> {
            if (item.getDueDate() != null) {
                return createCenteredCell(new Span(item.getDueDate().format(dateFmt)));
            }
            return createCenteredCell(new Span("-"));
        }));

        // Tình trạng hạn (Quá hạn / Còn lại)
        loanItemsDataGrid.getColumnByKey("overdueStatus").setRenderer(new ComponentRenderer<>(item -> {
            String status = item.getStatus();
            LocalDate due = item.getDueDate();
            LocalDate today = LocalDate.now();

            Span badge = new Span();
            badge.getStyle().set("padding", "3px 8px")
                    .set("border-radius", "12px")
                    .set("font-size", "12px")
                    .set("font-weight", "600");

            if (LoanItemStatus.BORROWED.getId().equalsIgnoreCase(status)) {
                if (due != null && today.isAfter(due)) {
                    long days = ChronoUnit.DAYS.between(due, today);
                    badge.setText(" Quá hạn " + days + " ngày");
                    badge.getStyle().set("background-color", "#fee2e2").set("color", "#dc2626");
                } else if (due != null) {
                    long remaining = ChronoUnit.DAYS.between(today, due);
                    if (remaining == 0) {
                        badge.setText(" Hết hạn hôm nay");
                        badge.getStyle().set("background-color", "#fef3c7").set("color", "#d97706");
                    } else {
                        badge.setText(" Còn " + remaining + " ngày");
                        badge.getStyle().set("background-color", "#e0f2fe").set("color", "#0284c7");
                    }
                }
            } else if (LoanItemStatus.RETURNED.getId().equalsIgnoreCase(status)) {
                LocalDate returnDate = item.getReturnedAt() != null
                        ? item.getReturnedAt().toLocalDate()
                        : null;
                if (due != null && returnDate != null && returnDate.isAfter(due)) {
                    long overdueDays = ChronoUnit.DAYS.between(due, returnDate);
                    badge.setText("Trả trễ " + overdueDays + " ngày");
                    badge.getStyle().set("background-color", "#fef3c7").set("color", "#b45309");
                } else {
                    badge.setText("Đúng hạn");
                    badge.getStyle().set("background-color", "#dcfce7").set("color", "#15803d");
                }
            } else if (LoanItemStatus.LOST.getId().equalsIgnoreCase(status)) {
                badge.setText("Mất sách");
                badge.getStyle().set("background-color", "#f3f4f6").set("color", "#6b7280");
            }
            return createCenteredCell(badge);
        }));

        // Lần gia hạn căn giữa
        loanItemsDataGrid.getColumnByKey("renewCount").setRenderer(new ComponentRenderer<>(item -> {
            return createCenteredCell(new Span(String.valueOf(item.getRenewCount() != null ? item.getRenewCount() : 0)));
        }));

        // Trạng thái mượn trả
        loanItemsDataGrid.getColumnByKey("itemStatus").setRenderer(new ComponentRenderer<>(item -> {
            String st = item.getStatus();
            Span badge = new Span();
            badge.getStyle().set("padding", "3px 8px")
                    .set("border-radius", "6px")
                    .set("font-size", "12px")
                    .set("font-weight", "500");

            if (LoanItemStatus.BORROWED.getId().equalsIgnoreCase(st)) {
                badge.setText("Đang mượn");
                badge.getStyle().set("background-color", "#eff6ff").set("color", "#1d4ed8");
            } else if (LoanItemStatus.RETURNED.getId().equalsIgnoreCase(st)) {
                badge.setText("Đã trả");
                badge.getStyle().set("background-color", "#f0fdf4").set("color", "#16a34a");
            } else if (LoanItemStatus.LOST.getId().equalsIgnoreCase(st)) {
                badge.setText("Đã mất");
                badge.getStyle().set("background-color", "#fef2f2").set("color", "#b91c1c");
            }
            return createCenteredCell(badge);
        }));

        // Ngày mượn định dạng gọn
        loanItemsDataGrid.getColumnByKey("loan.loanDate").setRenderer(new ComponentRenderer<>(item -> {
            if (item.getLoan() != null && item.getLoan().getLoanDate() != null) {
                return createCenteredCell(new Span(item.getLoan().getLoanDate().format(dtf)));
            }
            return createCenteredCell(new Span("-"));
        }));

        // Ngày trả sạch sẽ (bỏ hoàn toàn +0700)
        loanItemsDataGrid.getColumnByKey("returnedAt").setRenderer(new ComponentRenderer<>(item -> {
            if (item.getReturnedAt() != null) {
                return createCenteredCell(new Span(item.getReturnedAt().format(dtf)));
            }
            return createCenteredCell(new Span("-"));
        }));

        // Tình trạng khi trả bằng tiếng Việt
        loanItemsDataGrid.getColumnByKey("returnCondition").setRenderer(new ComponentRenderer<>(item -> {
            String cond = item.getReturnCondition();
            if (cond == null || cond.trim().isEmpty()) {
                return createCenteredCell(new Span("-"));
            }
            Span badge = new Span();
            badge.getStyle().set("padding", "3px 8px")
                    .set("border-radius", "6px")
                    .set("font-size", "12px")
                    .set("font-weight", "500");

            if (ReturnCondition.GOOD.getId().equalsIgnoreCase(cond)) {
                badge.setText("Tốt");
                badge.getStyle().set("background-color", "#f0fdf4").set("color", "#15803d");
            } else if (ReturnCondition.DAMAGED.getId().equalsIgnoreCase(cond)) {
                badge.setText("Hư hỏng");
                badge.getStyle().set("background-color", "#fef3c7").set("color", "#b45309");
            } else if (ReturnCondition.LOST.getId().equalsIgnoreCase(cond)) {
                badge.setText("Bị mất");
                badge.getStyle().set("background-color", "#fee2e2").set("color", "#b91c1c");
            } else {
                badge.setText(cond);
            }
            return createCenteredCell(badge);
        }));

        // Thủ thư nhận trả
        loanItemsDataGrid.getColumnByKey("receivedBy.username").setRenderer(new ComponentRenderer<>(item -> {
            if (item.getReceivedBy() != null && item.getReceivedBy().getUsername() != null) {
                return createCenteredCell(new Span(item.getReceivedBy().getUsername()));
            }
            return createCenteredCell(new Span("-"));
        }));
    }

    private void loadData() {
        String filterValue = statusFilterSelect.getValue();
        String statusParam = null;
        Boolean overdueOnly = null;

        if ("Đang mượn".equals(filterValue)) {
            statusParam = LoanItemStatus.BORROWED.getId();
        } else if ("Quá hạn".equals(filterValue)) {
            overdueOnly = Boolean.TRUE;
        } else if ("Đã trả".equals(filterValue)) {
            statusParam = LoanItemStatus.RETURNED.getId();
        } else if ("Đã mất".equals(filterValue)) {
            statusParam = LoanItemStatus.LOST.getId();
        }

        loanItemsDl.setParameter("statusFilter", statusParam);
        loanItemsDl.setParameter("overdueOnly", overdueOnly);
        loanItemsDl.setParameter("currentDate", LocalDate.now());
        loanItemsDl.setParameter("searchText", searchField.getValue() != null ? searchField.getValue().trim() : "");
        loanItemsDl.load();

        updateStatsRibbon();
    }

    private void updateStatsRibbon() {
        if (kpiContainer == null) return;
        kpiContainer.removeAll();

        Long totalBorrowed = dataManager.loadValue(
                "select count(li) from LoanItem li where li.status = 'BORROWED'", Long.class)
                .one();
        Long totalOverdue = dataManager.loadValue(
                "select count(li) from LoanItem li where li.status = 'BORROWED' and li.dueDate < :today", Long.class)
                .parameter("today", LocalDate.now())
                .one();
        Long returnedToday = dataManager.loadValue(
                "select count(li) from LoanItem li where li.status = 'RETURNED' and li.returnedAt >= :startToday",
                Long.class)
                .parameter("startToday", LocalDate.now().atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                .one();
        Long totalLost = dataManager.loadValue(
                "select count(li) from LoanItem li where li.status = 'LOST'", Long.class)
                .one();

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        long borrowed = totalBorrowed != null ? totalBorrowed : 0;
        long overdue = totalOverdue != null ? totalOverdue : 0;
        long returned = returnedToday != null ? returnedToday : 0;
        long lost = totalLost != null ? totalLost : 0;

        grid.add(createKpiCard(
                "Sách Đang Cho Mượn",
                String.valueOf(borrowed),
                "Độc giả đang lưu giữ",
                "bg-base border-contrast-10",
                "text-primary"
        ));

        grid.add(createKpiCard(
                "Sách Quá Hạn",
                String.valueOf(overdue),
                overdue > 0 ? "Cần liên hệ đôn đốc nhắc trả" : "Không có sách quá hạn",
                overdue > 0 ? "bg-error-10 text-error border-error-50" : "bg-base border-contrast-10",
                overdue > 0 ? "text-error font-bold" : "text-secondary"
        ));

        grid.add(createKpiCard(
                "Đã Trả Hôm Nay",
                String.valueOf(returned),
                "Lượt nhận trả trong ngày",
                returned > 0 ? "bg-success-10 text-success border-success-30" : "bg-base border-contrast-10",
                returned > 0 ? "text-success font-bold" : "text-primary"
        ));

        grid.add(createKpiCard(
                "Sách Đã Báo Mất",
                String.valueOf(lost),
                "Đã bồi thường / hỏng mất",
                lost > 0 ? "bg-warning-10 text-warning border-warning-20" : "bg-base border-contrast-10",
                lost > 0 ? "text-warning font-bold" : "text-primary"
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

    private void onCreateLoanClick(ClickEvent<Button> event) {
        viewNavigators.view(this, LoanCreateView.class).navigate();
    }

    private void onViewDetailClick(ClickEvent<Button> event) {
        LoanItem selected = loanItemsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 cuốn sách trên bảng để xem chi tiết phiếu mượn!");
            return;
        }
        openLoanDetail(selected);
    }

    private void openLoanDetail(LoanItem item) {
        if (item == null || item.getLoan() == null)
            return;
        viewNavigators.detailView(this, Loan.class)
                .withViewClass(LoanDetailView.class)
                .editEntity(item.getLoan())
                .navigate();
    }

    private void onReturnBookClick(ClickEvent<Button> event) {
        LoanItem selected = loanItemsDataGrid.getSingleSelectedItem();
        String initialBarcode = (selected != null && selected.getBookCopy() != null)
                ? selected.getBookCopy().getBarcode()
                : "";
        openReturnDialog(initialBarcode);
    }

    private void onRenewClick(ClickEvent<Button> event) {
        LoanItem selected = loanItemsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 cuốn sách đang mượn trên bảng để gia hạn!");
            return;
        }
        if (!LoanItemStatus.BORROWED.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Chỉ có thể gia hạn cuốn sách đang mượn!");
            return;
        }
        openRenewDialog(selected);
    }

    /**
     * Dialog Tiếp nhận trả sách (L05)
     */
    private void openReturnDialog(String prefillBarcode) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Tiếp Nhận Trả Sách Tại Quầy");
        dialog.setWidth("42em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        TextField barcodeField = new TextField("Mã vạch bản sao sách (Barcode)");
        barcodeField.setPlaceholder("Quét mã vạch hoặc nhập mã...");
        barcodeField.setWidthFull();
        barcodeField.setRequired(true);
        barcodeField.setClearButtonVisible(true);
        if (prefillBarcode != null) {
            barcodeField.setValue(prefillBarcode);
        }

        Div previewCard = new Div();
        previewCard.getStyle()
                .set("background", "#f8fafc")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "8px")
                .set("padding", "12px")
                .set("display", "none");

        RadioButtonGroup<ReturnCondition> conditionGroup = new RadioButtonGroup<>();
        conditionGroup.setLabel("Tình trạng sách khi trả");
        conditionGroup.setItems(ReturnCondition.GOOD, ReturnCondition.DAMAGED, ReturnCondition.LOST);
        conditionGroup.setItemLabelGenerator(ReturnCondition::getCaption);
        conditionGroup.setValue(ReturnCondition.GOOD);

        TextField noteField = new TextField("Ghi chú nhận trả");
        noteField.setWidthFull();
        noteField.setPlaceholder("Nhập ghi chú thêm nếu có...");

        Runnable checkBarcode = () -> {
            String code = barcodeField.getValue();
            if (code == null || code.trim().isEmpty()) {
                previewCard.getStyle().set("display", "none");
                return;
            }
            Optional<LoanItem> opt = dataManager.load(LoanItem.class)
                    .query("select li from LoanItem li where li.bookCopy.barcode = :code and li.status = 'BORROWED'")
                    .parameter("code", code.trim())
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                            .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE)
                                    .add("member",
                                            memberPlan -> memberPlan.addFetchPlan(FetchPlan.BASE).add("memberType",
                                                    FetchPlan.BASE)))
                            .add("bookCopy",
                                    copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE)))
                    .optional();

            previewCard.removeAll();
            if (opt.isPresent()) {
                LoanItem item = opt.get();
                previewCard.getStyle().set("display", "block");

                String bookTitle = item.getBookCopy().getBook() != null ? item.getBookCopy().getBook().getTitle() : "";
                String memberName = item.getLoan().getMember() != null ? item.getLoan().getMember().getFullName() : "";
                String memberCode = item.getLoan().getMember() != null ? item.getLoan().getMember().getMemberCode()
                        : "";
                LocalDate dueDate = item.getDueDate();
                LocalDate today = LocalDate.now();

                previewCard.add(new H4(" Cuốn sách: " + bookTitle));
                Div info = new Div(new Span(String.format("Độc giả: %s (%s) | Mã phiếu: %s | Hạn trả: %s",
                        memberName, memberCode, item.getLoan().getLoanCode(), dueDate)));
                info.getStyle().set("font-size", "13px").set("color", "#475569");
                previewCard.add(info);

                if (today.isAfter(dueDate)) {
                    long days = ChronoUnit.DAYS.between(dueDate, today);
                    MemberType mt = item.getLoan().getMember().getMemberType();
                    BigDecimal finePerDay = mt != null && mt.getFinePerDay() != null ? mt.getFinePerDay()
                            : BigDecimal.valueOf(5000);
                    BigDecimal overdueFine = finePerDay.multiply(BigDecimal.valueOf(days));

                    Div overdueAlert = new Div();
                    overdueAlert.getStyle()
                            .set("margin-top", "8px")
                            .set("padding", "8px 12px")
                            .set("background", "#fee2e2")
                            .set("color", "#b91c1c")
                            .set("border-radius", "6px")
                            .set("font-size", "13px")
                            .set("font-weight", "600");
                    overdueAlert.setText(String.format(
                            " CẢNH BÁO: Quá hạn %d ngày! Tiền phạt quá hạn ước tính: %,.0f VNĐ", days, overdueFine));
                    previewCard.add(overdueAlert);
                }
            } else {
                previewCard.getStyle().set("display", "block");
                Span notFound = new Span(" Không tìm thấy lượt mượn đang hoạt động cho mã vạch này!");
                notFound.getStyle().set("color", "#dc2626").set("font-size", "13px");
                previewCard.add(notFound);
            }
        };

        barcodeField.addValueChangeListener(e -> checkBarcode.run());
        if (prefillBarcode != null && !prefillBarcode.isEmpty()) {
            checkBarcode.run();
        }

        layout.add(barcodeField, previewCard, conditionGroup, noteField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận Trả sách", e -> {
            String code = barcodeField.getValue();
            if (code == null || code.trim().isEmpty()) {
                toastService.warning("Vui lòng quét hoặc nhập mã vạch!");
                return;
            }
            try {
                User user = (User) currentAuthentication.getUser();
                LoanService.ReturnResult res = loanService.returnBookCopy(
                        code.trim(), conditionGroup.getValue(), user, noteField.getValue());

                dialog.close();
                loadData();

                if (res.getReservedFor() != null) {
                    toastService.info(String.format(
                            "Nhận trả '%s' thành công! LƯU Ý: Sách được chuyển sang GIỮ CHỖ (READY) cho độc giả: %s",
                            res.getBookTitle(), res.getReservedFor()));
                } else if (res.isFineCreated() && res.getTotalFineAmount().compareTo(BigDecimal.ZERO) > 0) {
                    toastService.warning(String.format(
                            "Đã nhận trả sách thành công! Độc giả phát sinh tiền phạt: %,.0f VNĐ",
                            res.getTotalFineAmount()));
                } else {
                    toastService.success(String.format("Nhận trả sách '%s' thành công!", res.getBookTitle()));
                }
            } catch (Exception ex) {
                toastService.error("Lỗi khi trả sách: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Đóng", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    /**
     * Dialog Gia hạn sách (L06)
     */
    private void openRenewDialog(LoanItem item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Gia Hạn Mượn Sách Tại Quầy");
        dialog.setWidth("38em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        String bookTitle = item.getBookCopy() != null && item.getBookCopy().getBook() != null
                ? item.getBookCopy().getBook().getTitle()
                : "";
        String barcode = item.getBookCopy() != null ? item.getBookCopy().getBarcode() : "";
        String memberName = item.getLoan() != null && item.getLoan().getMember() != null
                ? item.getLoan().getMember().getFullName()
                : "";

        MemberType type = item.getLoan() != null && item.getLoan().getMember() != null
                ? item.getLoan().getMember().getMemberType()
                : null;
        int maxRenewals = type != null && type.getMaxRenewals() != null ? type.getMaxRenewals() : 1;
        int renewDays = type != null && type.getRenewDays() != null ? type.getRenewDays() : 7;
        int currentRenewCount = item.getRenewCount() != null ? item.getRenewCount() : 0;
        LocalDate oldDue = item.getDueDate();
        LocalDate newDue = oldDue != null ? oldDue.plusDays(renewDays) : LocalDate.now().plusDays(renewDays);

        Div card = new Div();
        card.getStyle()
                .set("background", "#f8fafc")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "8px")
                .set("padding", "16px");

        H4 title = new H4(bookTitle);
        title.getStyle().set("margin-top", "0").set("color", "#1e293b");

        FormLayout fl = new FormLayout();
        fl.addFormItem(new Span(barcode), "Mã vạch");
        fl.addFormItem(new Span(memberName), "Độc giả");
        fl.addFormItem(new Span(oldDue != null ? oldDue.toString() : ""), "Hạn trả hiện tại");
        fl.addFormItem(new Span(currentRenewCount + " / " + maxRenewals + " lần"), "Số lần đã gia hạn");

        Span newDueSpan = new Span(newDue.toString() + String.format(" (+%d ngày)", renewDays));
        newDueSpan.getStyle().set("font-weight", "700").set("color", "#059669");
        fl.addFormItem(newDueSpan, "Hạn trả mới dự kiến");

        card.add(title, fl);

        TextField noteField = new TextField("Ghi chú gia hạn");
        noteField.setWidthFull();
        noteField.setPlaceholder("Nhập lý do hoặc ghi chú nếu có...");

        layout.add(card, noteField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận Gia hạn", e -> {
            try {
                User user = (User) currentAuthentication.getUser();
                loanService.renewLoanItem(item.getId(), user, RenewalChannel.COUNTER, noteField.getValue());
                dialog.close();
                loadData();
                toastService.success(String.format("Gia hạn thành công! Hạn mới: %s", newDue));
            } catch (Exception ex) {
                toastService.error("Không thể gia hạn: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Hủy", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }
}
