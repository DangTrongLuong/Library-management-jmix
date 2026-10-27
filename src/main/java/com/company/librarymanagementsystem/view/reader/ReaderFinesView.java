package com.company.librarymanagementsystem.view.reader;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.ReaderLoanService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "reader/my-fines", layout = MainView.class)
@ViewController(id = "ReaderFinesView")
@ViewDescriptor(path = "reader-fines-view.xml")
public class ReaderFinesView extends StandardView {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @ViewComponent
    private Button viewModeFinesBtn;
    @ViewComponent
    private Button viewModePaymentsBtn;
    @ViewComponent
    private Button refreshBtn;

    @ViewComponent
    private Div kpiContainer;
    @ViewComponent
    private Div guidanceBanner;

    @ViewComponent
    private VerticalLayout finesContainer;
    @ViewComponent
    private TextField fineSearchField;
    @ViewComponent
    private ComboBox<String> fineStatusFilter;
    @ViewComponent
    private Button clearFineFilterBtn;
    @ViewComponent
    private Span finesCountLabel;
    @ViewComponent
    private Div finesGridWrapper;
    @ViewComponent
    private Div emptyFinesState;

    @ViewComponent
    private VerticalLayout paymentsContainer;
    @ViewComponent
    private Div paymentsGridWrapper;
    @ViewComponent
    private Div emptyPaymentsState;

    @Autowired
    private ReaderLoanService readerLoanService;

    private Member currentMember;
    private List<Fine> allFines = new ArrayList<>();
    private List<Payment> allPayments = new ArrayList<>();
    private boolean isFinesTab = true;

    @Subscribe
    public void onInit(InitEvent event) {
        viewModeFinesBtn.addClickListener(e -> switchTab(true));
        viewModePaymentsBtn.addClickListener(e -> switchTab(false));
        refreshBtn.addClickListener(e -> reloadData());

        fineStatusFilter.setItems("Tất cả trạng thái", "Chưa thanh toán / Còn nợ", "Đã thanh toán đủ", "Đã miễn giảm");
        fineStatusFilter.setValue("Tất cả trạng thái");
        fineStatusFilter.addValueChangeListener(e -> applyFinesFilter());

        fineSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        fineSearchField.addValueChangeListener(e -> applyFinesFilter());

        clearFineFilterBtn.addClickListener(e -> {
            fineSearchField.clear();
            fineStatusFilter.setValue("Tất cả trạng thái");
            applyFinesFilter();
        });
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        reloadData();
    }

    private void switchTab(boolean finesTab) {
        this.isFinesTab = finesTab;
        if (finesTab) {
            viewModeFinesBtn.setThemeName("primary small");
            viewModePaymentsBtn.setThemeName("tertiary small");
            finesContainer.setVisible(true);
            paymentsContainer.setVisible(false);
        } else {
            viewModeFinesBtn.setThemeName("tertiary small");
            viewModePaymentsBtn.setThemeName("primary small");
            finesContainer.setVisible(false);
            paymentsContainer.setVisible(true);
        }
    }

    private void reloadData() {
        Optional<Member> memberOpt = readerLoanService.getCurrentMember();
        if (memberOpt.isEmpty()) {
            renderNoMemberState();
            return;
        }

        this.currentMember = memberOpt.get();
        this.allFines = readerLoanService.getReaderFines(currentMember.getId());
        this.allPayments = readerLoanService.getReaderPayments(currentMember.getId());

        renderKpis();
        renderGuidanceBanner();
        applyFinesFilter();
        renderPaymentsGrid();
    }

    private void renderNoMemberState() {
        kpiContainer.removeAll();
        guidanceBanner.removeAll();
        finesGridWrapper.removeAll();
        paymentsGridWrapper.removeAll();

        Div alert = new Div();
        alert.addClassNames("bg-warning-10", "border", "border-warning-30", "rounded-m", "p-m", "text-center", "box-border", "flex", "flex-col", "items-center", "gap-s");
        Span msg = new Span(
                "Tài khoản của bạn chưa được liên kết với hồ sơ độc giả thư viện. Bạn có thể liên kết thẻ thư viện đã làm tại quầy để tra cứu công nợ và biên lai.");
        msg.addClassNames("text-warning", "font-medium", "text-s");

        Button linkBtn = new Button("Đi đến trang Liên kết Thẻ Thư Viện", VaadinIcon.LINK.create(), e -> UI.getCurrent().navigate("reader-profile"));
        linkBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        alert.add(msg, linkBtn);
        kpiContainer.add(alert);
    }

    private void renderKpis() {
        kpiContainer.removeAll();
        ReaderLoanService.ReaderFineSummaryDto summary = readerLoanService.getFineSummary(currentMember.getId());

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        boolean hasDebt = summary.totalOutstanding().compareTo(BigDecimal.ZERO) > 0;

        // 1. Dư nợ cần nộp
        Div debtCard = new Div();
        debtCard.addClassNames("rounded-m", "border", "p-s", "shadow-xs", "flex", "flex-col", "justify-between",
                "box-border");
        debtCard.getStyle().set("box-sizing", "border-box").set("min-width", "0");
        if (hasDebt) {
            debtCard.addClassNames("bg-error-10", "border-error-50");
        } else {
            debtCard.addClassNames("bg-success-10", "border-success-50");
        }

        Span debtLabel = new Span("DƯ NỢ CẦN NỘP");
        debtLabel.addClassNames(hasDebt ? "text-error" : "text-success", "font-medium", "uppercase");
        debtLabel.getStyle().set("font-size", "10.5px");

        Span debtVal = new Span(String.format("%,.0f VNĐ", summary.totalOutstanding()));
        debtVal.addClassNames("font-bold", "my-xs", hasDebt ? "text-error" : "text-success");
        debtVal.getStyle().set("font-size", "18px");

        Span debtSub = new Span(hasDebt ? "Cần thanh toán tại quầy" : "Không có nợ phạt");
        debtSub.addClassNames(hasDebt ? "text-error" : "text-success", "font-medium");
        debtSub.getStyle().set("font-size", "11px");

        debtCard.add(debtLabel, debtVal, debtSub);
        grid.add(debtCard);

        // 2. Tổng phạt phát sinh
        grid.add(createKpiCard(
                "Tổng Phạt Phát Sinh",
                String.format("%,.0f VNĐ", summary.totalAmount()),
                summary.totalFinesCount() + " khoản ghi nhận",
                "bg-base border-contrast-10",
                "text-primary"));

        // 3. Đã thanh toán
        grid.add(createKpiCard(
                "Đã Thanh Toán",
                String.format("%,.0f VNĐ", summary.totalPaid()),
                allPayments.size() + " biên lai đã nộp",
                "bg-base border-contrast-10",
                "text-success"));

        // 4. Được miễn giảm
        grid.add(createKpiCard(
                "Được Miễn Giảm",
                String.format("%,.0f VNĐ", summary.totalWaived()),
                "Miễn giảm theo quy định",
                "bg-base border-contrast-10",
                "text-secondary"));

        kpiContainer.add(grid);
    }

    private Div createKpiCard(String label, String value, String subtext, String boxClass, String valueClass) {
        Div card = new Div();
        card.addClassNames("bg-base", "rounded-m", "border", "p-s", "shadow-xs", "flex", "flex-col", "justify-between",
                "box-border");
        card.getStyle().set("box-sizing", "border-box").set("min-width", "0");
        for (String c : boxClass.split(" ")) {
            if (!c.isBlank())
                card.addClassName(c);
        }

        Span lbl = new Span(label);
        lbl.addClassNames("text-secondary", "font-medium", "uppercase");
        lbl.getStyle().set("font-size", "10.5px");

        Span val = new Span(value);
        val.addClassNames("font-bold", "my-xs");
        val.getStyle().set("font-size", "18px");
        for (String c : valueClass.split(" ")) {
            if (!c.isBlank())
                val.addClassName(c);
        }

        Span sub = new Span(subtext);
        sub.addClassNames("text-secondary");
        sub.getStyle().set("font-size", "11px");

        card.add(lbl, val, sub);
        return card;
    }

    private void renderGuidanceBanner() {
        guidanceBanner.removeAll();

        Div banner = new Div();
        banner.addClassNames("bg-base", "rounded-m", "border", "border-contrast-10", "p-s", "shadow-xs", "box-border");
        banner.getStyle()
                .set("box-sizing", "border-box")
                .set("width", "100%")
                .set("max-width", "100%");

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(260px, 1fr))")
                .set("gap", "0.75rem")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        // Quầy thanh toán
        Div deskCard = new Div();
        deskCard.addClassNames("flex", "items-center", "gap-s", "box-border");
        deskCard.getStyle().set("min-width", "0").set("box-sizing", "border-box");

        Span deskIcon = new Span();
        deskIcon.getStyle().set("font-size", "1.5rem").set("flex-shrink", "0");

        Div deskText = new Div();
        deskText.getStyle().set("min-width", "0").set("overflow", "hidden");

        Span deskTitle = new Span("Quầy Lưu Thông Thư Viện (Tầng 1)");
        deskTitle.addClassNames("font-bold", "text-primary", "block");
        deskTitle.getStyle().set("font-size", "12.5px");

        Span deskDesc = new Span("Nộp trực tiếp tiền mặt hoặc POS (08:00 - 18:00 T2-T7)");
        deskDesc.addClassNames("text-secondary", "block");
        deskDesc.getStyle().set("font-size", "11px");

        deskText.add(deskTitle, deskDesc);
        deskCard.add(deskIcon, deskText);

        grid.add(deskCard);
        banner.add(grid);
        guidanceBanner.add(banner);
    }

    private void applyFinesFilter() {
        String keyword = fineSearchField.getValue() != null ? fineSearchField.getValue().trim().toLowerCase() : "";
        String statusOpt = fineStatusFilter.getValue();

        List<Fine> filtered = allFines.stream().filter(f -> {
            // Lọc theo trạng thái
            if ("Chưa thanh toán / Còn nợ".equals(statusOpt)) {
                if (!FineStatus.UNPAID.getId().equalsIgnoreCase(f.getStatus())
                        && !FineStatus.PARTIAL.getId().equalsIgnoreCase(f.getStatus())) {
                    return false;
                }
            } else if ("Đã thanh toán đủ".equals(statusOpt)) {
                if (!FineStatus.PAID.getId().equalsIgnoreCase(f.getStatus())) {
                    return false;
                }
            } else if ("Đã miễn giảm".equals(statusOpt)) {
                if (!FineStatus.WAIVED.getId().equalsIgnoreCase(f.getStatus())) {
                    return false;
                }
            }

            // Lọc theo từ khóa
            if (!keyword.isEmpty()) {
                String desc = f.getDescription() != null ? f.getDescription().toLowerCase() : "";
                String type = f.getFineType() != null ? f.getFineType().toLowerCase() : "";
                String bookTitle = (f.getLoanItem() != null && f.getLoanItem().getBookCopy() != null
                        && f.getLoanItem().getBookCopy().getBook() != null)
                                ? f.getLoanItem().getBookCopy().getBook().getTitle().toLowerCase()
                                : "";
                String barcode = (f.getLoanItem() != null && f.getLoanItem().getBookCopy() != null)
                        ? f.getLoanItem().getBookCopy().getBarcode().toLowerCase()
                        : "";

                return desc.contains(keyword) || type.contains(keyword) || bookTitle.contains(keyword)
                        || barcode.contains(keyword);
            }
            return true;
        }).collect(Collectors.toList());

        finesCountLabel.setText("Hiển thị: " + filtered.size() + " / " + allFines.size() + " khoản");
        renderFinesTable(filtered);
    }

    private void renderFinesTable(List<Fine> list) {
        finesGridWrapper.removeAll();
        emptyFinesState.removeAll();

        if (list.isEmpty()) {
            emptyFinesState.setVisible(true);
            finesGridWrapper.setVisible(false);

            Div emptyBox = new Div();
            emptyBox.setWidthFull();
            emptyBox.addClassNames("bg-base", "rounded-l", "border", "border-contrast-10", "p-xl", "text-center",
                    "shadow-s");

            H3 title = new H3("Không có khoản phạt nào");
            title.addClassNames("font-bold", "text-primary", "mb-xs");
            title.getStyle().set("font-size", "14px");

            Paragraph desc = new Paragraph(
                    "Thẻ thư viện của bạn hiện không có ghi nhận khoản nợ hoặc phạt dịch vụ nào phù hợp với bộ lọc.");
            desc.addClassNames("text-secondary", "max-w-md", "mx-auto");
            desc.getStyle().set("font-size", "12px");

            emptyBox.add(title, desc);
            emptyFinesState.add(emptyBox);
            return;
        }

        emptyFinesState.setVisible(false);
        finesGridWrapper.setVisible(true);

        Grid<Fine> grid = new Grid<>();
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addClassNames("border-none");

        // Cột 1: Loại phạt
        grid.addColumn(new ComponentRenderer<>(fine -> {
            Span pill = new Span();
            pill.getStyle().set("font-size", "11px");
            pill.addClassNames("px-s", "py-xs", "rounded-full", "font-semibold");

            String t = fine.getFineType();
            if (FineType.OVERDUE.getId().equalsIgnoreCase(t)) {
                pill.setText("Quá hạn mượn");
                pill.addClassNames("bg-warning-10", "text-warning");
            } else if (FineType.LOST.getId().equalsIgnoreCase(t)) {
                pill.setText("Bồi thường mất");
                pill.addClassNames("bg-error-10", "text-error");
            } else if (FineType.DAMAGED.getId().equalsIgnoreCase(t)) {
                pill.setText("Hỏng sách");
                pill.addClassNames("bg-contrast-10", "text-secondary");
            } else {
                pill.setText(t != null ? t : "Phí dịch vụ");
                pill.addClassNames("bg-contrast-10", "text-secondary");
            }
            return pill;
        })).setHeader("Loại Phạt").setAutoWidth(true).setFlexGrow(1);

        // Cột 2: Sách & Mã vạch liên quan
        grid.addColumn(new ComponentRenderer<>(fine -> {
            VerticalLayout v = new VerticalLayout();
            v.setPadding(false);
            v.setSpacing(false);

            String bookTitle = (fine.getLoanItem() != null && fine.getLoanItem().getBookCopy() != null
                    && fine.getLoanItem().getBookCopy().getBook() != null)
                            ? fine.getLoanItem().getBookCopy().getBook().getTitle()
                            : fine.getDescription();

            Span titleSpan = new Span(bookTitle != null ? bookTitle : "Khoản phạt dịch vụ");
            titleSpan.addClassNames("font-medium", "text-primary");
            titleSpan.getStyle().set("font-size", "12.5px");

            String barcode = (fine.getLoanItem() != null && fine.getLoanItem().getBookCopy() != null)
                    ? "Mã vạch: " + fine.getLoanItem().getBookCopy().getBarcode()
                    : "";
            Span barcodeSpan = new Span(barcode);
            barcodeSpan.addClassNames("text-secondary");
            barcodeSpan.getStyle().set("font-size", "11px");

            v.add(titleSpan, barcodeSpan);
            return v;
        })).setHeader("Sách & Chi Tiết").setAutoWidth(true).setFlexGrow(2);

        // Cột 3: Quá hạn
        grid.addColumn(fine -> {
            if (fine.getDaysOverdue() != null && fine.getDaysOverdue() > 0) {
                return fine.getDaysOverdue() + " ngày";
            }
            return "-";
        }).setHeader("Trễ Hạn").setAutoWidth(true).setFlexGrow(1);

        // Cột 4: Số tiền phạt
        grid.addColumn(
                fine -> String.format("%,.0f VNĐ", fine.getAmount() != null ? fine.getAmount() : BigDecimal.ZERO))
                .setHeader("Số Tiền Phạt")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Cột 5: Đã nộp
        grid.addColumn(fine -> String.format("%,.0f VNĐ",
                fine.getPaidAmount() != null ? fine.getPaidAmount() : BigDecimal.ZERO))
                .setHeader("Đã Nộp")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Cột 6: Còn nợ
        grid.addColumn(new ComponentRenderer<>(fine -> {
            BigDecimal rem = fine.getRemainingAmount();
            Span span = new Span(String.format("%,.0f VNĐ", rem));
            span.getStyle().set("font-size", "12.5px");
            if (rem.compareTo(BigDecimal.ZERO) > 0) {
                span.addClassNames("font-bold", "text-error");
            } else {
                span.addClassNames("text-secondary");
            }
            return span;
        })).setHeader("Còn Nợ").setAutoWidth(true).setFlexGrow(1);

        // Cột 7: Trạng thái
        grid.addColumn(new ComponentRenderer<>(fine -> {
            Span pill = new Span();
            pill.getStyle().set("font-size", "11px");
            pill.addClassNames("px-s", "py-xs", "rounded-full", "font-semibold");

            String st = fine.getStatus();
            if (FineStatus.UNPAID.getId().equalsIgnoreCase(st)) {
                pill.setText("Chưa nộp");
                pill.addClassNames("bg-error-10", "text-error");
            } else if (FineStatus.PARTIAL.getId().equalsIgnoreCase(st)) {
                pill.setText("Nộp 1 phần");
                pill.addClassNames("bg-warning-10", "text-warning");
            } else if (FineStatus.PAID.getId().equalsIgnoreCase(st)) {
                pill.setText("Đã thanh toán");
                pill.addClassNames("bg-success-10", "text-success");
            } else if (FineStatus.WAIVED.getId().equalsIgnoreCase(st)) {
                pill.setText("Đã miễn giảm");
                pill.addClassNames("bg-contrast-10", "text-secondary");
            } else {
                pill.setText(st);
                pill.addClassNames("bg-contrast-10", "text-secondary");
            }
            return pill;
        })).setHeader("Trạng Thái").setAutoWidth(true).setFlexGrow(1);

        // Cột 8: Ngày lập
        grid.addColumn(fine -> fine.getCreatedAt() != null ? fine.getCreatedAt().format(DATE_FMT) : "-")
                .setHeader("Ngày Lập")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.setItems(list);
        finesGridWrapper.add(grid);
    }

    private void renderPaymentsGrid() {
        paymentsGridWrapper.removeAll();
        emptyPaymentsState.removeAll();

        if (allPayments.isEmpty()) {
            emptyPaymentsState.setVisible(true);
            paymentsGridWrapper.setVisible(false);

            Div emptyBox = new Div();
            emptyBox.setWidthFull();
            emptyBox.addClassNames("bg-base", "rounded-l", "border", "border-contrast-10", "p-xl", "text-center",
                    "shadow-s");

            H3 title = new H3("Chưa có biên lai thanh toán");
            title.addClassNames("font-bold", "text-primary", "mb-xs");
            title.getStyle().set("font-size", "14px");

            Paragraph desc = new Paragraph(
                    "Sau khi bạn thanh toán tiền phạt tại quầy hoặc chuyển khoản thành công, biên lai điện tử sẽ được hiển thị đầy đủ tại đây.");
            desc.addClassNames("text-secondary", "max-w-md", "mx-auto");
            desc.getStyle().set("font-size", "12px");

            emptyBox.add(title, desc);
            emptyPaymentsState.add(emptyBox);
            return;
        }

        emptyPaymentsState.setVisible(false);
        paymentsGridWrapper.setVisible(true);

        Grid<Payment> grid = new Grid<>();
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addClassNames("border-none");

        // Cột 1: Số biên lai
        grid.addColumn(new ComponentRenderer<>(p -> {
            Span s = new Span(p.getReceiptNo());
            s.addClassNames("font-mono", "font-bold", "text-primary");
            s.getStyle().set("font-size", "12px");
            return s;
        })).setHeader("Số Biên Lai").setAutoWidth(true).setFlexGrow(1);

        // Cột 2: Thời gian nộp
        grid.addColumn(p -> p.getPaidAt() != null ? p.getPaidAt().format(DATETIME_FMT) : "-")
                .setHeader("Thời Gian Nộp")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Cột 3: Số tiền nộp
        grid.addColumn(new ComponentRenderer<>(p -> {
            Span s = new Span(String.format("%,.0f VNĐ", p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO));
            s.addClassNames("font-bold", "text-success");
            s.getStyle().set("font-size", "13px");
            return s;
        })).setHeader("Số Tiền Nộp").setAutoWidth(true).setFlexGrow(1);

        // Cột 4: Phương thức
        grid.addColumn(new ComponentRenderer<>(p -> {
            Span pill = new Span();
            pill.getStyle().set("font-size", "11px");
            pill.addClassNames("px-s", "py-xs", "rounded-full", "font-medium");

            String m = p.getMethod();
            if (PaymentMethod.CASH.getId().equalsIgnoreCase(m)) {
                pill.setText("Tiền mặt");
                pill.addClassNames("bg-success-10", "text-success");
            } else if (PaymentMethod.BANK_TRANSFER.getId().equalsIgnoreCase(m)) {
                pill.setText("Chuyển khoản");
                pill.addClassNames("bg-warning-10", "text-warning");
            } else if (PaymentMethod.CARD.getId().equalsIgnoreCase(m)) {
                pill.setText("Thẻ ngân hàng / POS");
                pill.addClassNames("bg-contrast-10", "text-primary");
            } else if (PaymentMethod.E_WALLET.getId().equalsIgnoreCase(m)) {
                pill.setText("Ví điện tử");
                pill.addClassNames("bg-contrast-10", "text-primary");
            } else {
                pill.setText(m != null ? m : "Khác");
                pill.addClassNames("bg-contrast-10", "text-secondary");
            }
            return pill;
        })).setHeader("Phương Thức").setAutoWidth(true).setFlexGrow(1);

        // Cột 5: Mã tham chiếu GD
        grid.addColumn(
                p -> p.getTransactionRef() != null && !p.getTransactionRef().isBlank() ? p.getTransactionRef() : "-")
                .setHeader("Mã Giao Dịch").setAutoWidth(true).setFlexGrow(1);

        // Cột 6: Thu ngân
        grid.addColumn(p -> p.getReceivedBy() != null ? p.getReceivedBy().getUsername() : "Hệ thống")
                .setHeader("Thu Ngân").setAutoWidth(true).setFlexGrow(1);

        // Cột 7: Ghi chú
        grid.addColumn(p -> p.getNote() != null && !p.getNote().isBlank() ? p.getNote() : "-")
                .setHeader("Ghi Chú").setAutoWidth(true).setFlexGrow(1);

        grid.setItems(allPayments);
        paymentsGridWrapper.add(grid);
    }
}
