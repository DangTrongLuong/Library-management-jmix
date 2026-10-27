package com.company.librarymanagementsystem.view.reader;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.ReaderLoanService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "reader/current-loans", layout = MainView.class)
@ViewController(id = "ReaderCurrentLoansView")
@ViewDescriptor(path = "reader-current-loans-view.xml")
public class ReaderCurrentLoansView extends StandardView {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DEFAULT_COVER = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=300&auto=format&fit=crop&q=80";

    @ViewComponent
    private Button refreshBtn;
    @ViewComponent
    private Button viewModeCardBtn;
    @ViewComponent
    private Button viewModeTableBtn;
    @ViewComponent
    private Div memberHeaderCard;
    @ViewComponent
    private VerticalLayout cardContainer;
    @ViewComponent
    private Div cardsGrid;
    @ViewComponent
    private VerticalLayout tableContainer;
    @ViewComponent
    private Div tableWrapper;

    @Autowired
    private ReaderLoanService readerLoanService;
    @Autowired
    private ToastService toastService;

    private boolean isCardMode = true;
    private Member currentMember;

    @Subscribe
    public void onInit(InitEvent event) {
        viewModeCardBtn.addClickListener(e -> switchViewMode(true));
        viewModeTableBtn.addClickListener(e -> switchViewMode(false));
        refreshBtn.addClickListener(e -> reloadData());
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        reloadData();
    }

    private void switchViewMode(boolean cardMode) {
        this.isCardMode = cardMode;
        if (cardMode) {
            viewModeCardBtn.setThemeName("primary small");
            viewModeTableBtn.setThemeName("tertiary small");
            cardContainer.setVisible(true);
            tableContainer.setVisible(false);
        } else {
            viewModeCardBtn.setThemeName("tertiary small");
            viewModeTableBtn.setThemeName("primary small");
            cardContainer.setVisible(false);
            tableContainer.setVisible(true);
        }
    }

    private void reloadData() {
        Optional<Member> memberOpt = readerLoanService.getCurrentMember();
        if (memberOpt.isEmpty()) {
            renderNoMemberCard();
            cardsGrid.removeAll();
            tableWrapper.removeAll();
            return;
        }

        this.currentMember = memberOpt.get();
        renderMemberHeader();
        renderLoansContent();
    }

    private void renderNoMemberCard() {
        memberHeaderCard.removeAll();
        Div banner = new Div();
        banner.addClassNames("bg-warning-10", "border", "border-warning-30", "rounded-m", "p-m", "text-center", "box-border", "flex", "flex-col", "items-center", "gap-s");
        
        H4 title = new H4("Tài khoản chưa liên kết Thẻ Độc giả");
        title.addClassNames("text-warning", "font-bold");
        title.getStyle().set("margin", "0 0 4px 0");
        
        Span desc = new Span("Nếu bạn đã làm thẻ tại quầy thư viện, vui lòng liên kết thẻ vào tài khoản này để quản lý sách mượn và gia hạn.");
        desc.addClassNames("text-secondary", "text-s");
        
        Button linkBtn = new Button("Liên kết Thẻ Thư Viện ngay", VaadinIcon.LINK.create(), e -> UI.getCurrent().navigate("reader-profile"));
        linkBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        
        banner.add(title, desc, linkBtn);
        memberHeaderCard.add(banner);
    }

    private void renderMemberHeader() {
        memberHeaderCard.removeAll();
        ReaderLoanService.LoanSummaryDto summary = readerLoanService.getLoanSummary(currentMember);

        // Hàng 1: Thông tin thẻ bạn đọc
        HorizontalLayout infoRow = new HorizontalLayout();
        infoRow.setWidthFull();
        infoRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        infoRow.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout memberBadge = new HorizontalLayout();
        memberBadge.setAlignItems(FlexComponent.Alignment.CENTER);
        memberBadge.setSpacing(true);

        Div avatarBox = new Div(com.vaadin.flow.component.icon.VaadinIcon.USER_CARD.create());
        avatarBox.getStyle()
                .set("background", "#eff6ff")
                .set("border-radius", "50%")
                .set("width", "42px")
                .set("height", "42px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "20px");

        VerticalLayout memberText = new VerticalLayout();
        memberText.setSpacing(false);
        memberText.setPadding(false);

        HorizontalLayout nameRow = new HorizontalLayout();
        nameRow.setAlignItems(FlexComponent.Alignment.CENTER);
        nameRow.setSpacing(true);

        H3 nameH3 = new H3(summary.fullName());
        nameH3.getStyle().set("margin", "0").set("color", "#1e293b").set("font-size", "18px");

        Span typeBadge = new Span(summary.memberTypeName());
        typeBadge.getStyle()
                .set("background", "#dbeafe")
                .set("color", "#1e40af")
                .set("padding", "2px 8px")
                .set("border-radius", "12px")
                .set("font-size", "11px")
                .set("font-weight", "600");

        nameRow.add(nameH3, typeBadge);

        String expiry = summary.cardExpiryDate() != null ? summary.cardExpiryDate().format(DATE_FMT) : "Vô thời hạn";
        Span metaSpan = new Span(String.format("Mã thẻ: %s  •  Hạn thẻ: %s", summary.memberCode(), expiry));
        metaSpan.getStyle().set("font-size", "13px").set("color", "#64748b");

        memberText.add(nameRow, metaSpan);
        memberBadge.add(avatarBox, memberText);

        infoRow.add(memberBadge);

        memberHeaderCard.getStyle()
                .set("box-sizing", "border-box")
                .set("max-width", "100%");

        // Hàng 2: Thống kê KPI dạng Grid co giãn đều theo màn hình
        Div kpiGrid = new Div();
        kpiGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box")
                .set("margin-top", "10px");

        boolean hasFines = summary.unpaidFines() != null && summary.unpaidFines().compareTo(BigDecimal.ZERO) > 0;

        kpiGrid.add(
                createKpiCard("Sách Đang Mượn",
                        String.format("%d / %d cuốn", summary.currentLoansCount(), summary.maxBorrowLimit()),
                        "Hạn mức mượn tối đa",
                        "bg-base border-contrast-10",
                        "text-primary"),
                createKpiCard("Sắp Đến Hạn",
                        String.format("%d cuốn", summary.dueSoonCount()),
                        "Trong vòng 3 ngày tới",
                        summary.dueSoonCount() > 0 ? "bg-warning-10 border-warning-20 text-warning" : "bg-base border-contrast-10",
                        summary.dueSoonCount() > 0 ? "text-warning" : "text-primary"),
                createKpiCard("Đã Quá Hạn",
                        String.format("%d cuốn", summary.overdueCount()),
                        summary.overdueCount() > 0 ? "Cần trả sớm tại quầy!" : "Không có sách trễ",
                        summary.overdueCount() > 0 ? "bg-error-10 border-error-50 text-error" : "bg-base border-contrast-10",
                        summary.overdueCount() > 0 ? "text-error" : "text-primary"),
                createKpiCard("Tiền Phạt Còn Nợ",
                        String.format("%,.0f VNĐ", summary.unpaidFines() != null ? summary.unpaidFines() : BigDecimal.ZERO),
                        hasFines ? "Cần nộp trước khi gia hạn" : "Không có nợ phạt",
                        hasFines ? "bg-error-10 border-error-50 text-error" : "bg-base border-contrast-10",
                        hasFines ? "text-error" : "text-primary"));

        memberHeaderCard.add(infoRow, kpiGrid);
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

    private void renderLoansContent() {
        cardsGrid.removeAll();
        tableWrapper.removeAll();

        List<ReaderLoanService.CurrentLoanItemDto> loans = readerLoanService.getCurrentLoans(currentMember.getId());

        if (loans.isEmpty()) {
            Div emptyDiv = new Div();
            emptyDiv.getStyle()
                    .set("text-align", "center")
                    .set("padding", "28px 16px")
                    .set("background", "#ffffff")
                    .set("border", "1px dashed #cbd5e1")
                    .set("border-radius", "8px")
                    .set("box-sizing", "border-box")
                    .set("width", "100%");

            H4 emptyTitle = new H4("Bạn hiện không mượn cuốn sách nào");
            emptyTitle.getStyle()
                    .set("font-size", "15px")
                    .set("font-weight", "600")
                    .set("color", "#1e293b")
                    .set("margin", "0 0 4px 0");

            Span desc = new Span("Hãy khám phá kho sách phong phú của thư viện và chọn sách bạn yêu thích nhé!");
            desc.getStyle()
                    .set("font-size", "12.5px")
                    .set("color", "#64748b")
                    .set("display", "block")
                    .set("margin-bottom", "14px");

            Button opacBtn = new Button("Tra cứu & Chọn sách ngay", e -> UI.getCurrent().navigate("opac"));
            opacBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

            emptyDiv.add(emptyTitle, desc, opacBtn);
            cardsGrid.add(emptyDiv);
            return;
        }

        // Render Card Grid: 1 hàng 2 hoặc 3 card rộng rãi
        cardsGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(400px, 1fr))")
                .set("gap", "18px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        for (ReaderLoanService.CurrentLoanItemDto dto : loans) {
            cardsGrid.add(createLoanCard(dto));
        }

        // Render DataGrid (Table view)
        renderLoansTable(loans);
    }

    private Div createLoanCard(ReaderLoanService.CurrentLoanItemDto dto) {
        Div card = new Div();
        card.addClassNames("bg-base", "rounded-l", "border", "shadow-s", "p-m", "flex", "flex-col", "justify-between", "transition-all");

        boolean isOverdue = dto.isOverdue();
        boolean isDueSoon = dto.daysRemaining() <= 3 && !isOverdue;

        if (isOverdue) {
            card.getStyle().set("border-color", "var(--lumo-error-color-50pct, #ef4444)");
        } else if (isDueSoon) {
            card.getStyle().set("border-color", "var(--lumo-warning-color-50pct, #f59e0b)");
        } else {
            card.getStyle().set("border-color", "var(--lumo-contrast-15pct, #e2e8f0)");
        }

        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle().set("transform", "translateY(-2px)").set("box-shadow", "0 6px 14px rgba(0,0,0,0.08)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle().set("transform", "translateY(0)").set("box-shadow", "0 2px 6px rgba(0,0,0,0.04)");
        });

        // --- 1. TOP BADGE & BORROW DATE ---
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.addClassNames("mb-s");

        Span statusBadge = new Span();
        if (isOverdue) {
            statusBadge.setText(String.format("QUÁ HẠN %d NGÀY", dto.daysOverdue()));
            statusBadge.addClassNames("bg-error-10", "text-error", "border", "border-error-30", "font-bold", "px-s", "py-xs", "rounded-full");
            statusBadge.getStyle().set("font-size", "11px");
        } else if (isDueSoon) {
            statusBadge.setText(String.format("CÒN %d NGÀY NỮA HẾT HẠN", dto.daysRemaining()));
            statusBadge.addClassNames("bg-warning-10", "text-warning", "border", "border-warning-30", "font-bold", "px-s", "py-xs", "rounded-full");
            statusBadge.getStyle().set("font-size", "11px");
        } else {
            statusBadge.setText(String.format("ĐANG MƯỢN • CÒN %d NGÀY", dto.daysRemaining()));
            statusBadge.addClassNames("bg-success-10", "text-success", "border", "border-success-30", "font-bold", "px-s", "py-xs", "rounded-full");
            statusBadge.getStyle().set("font-size", "11px");
        }

        String borrowDateStr = dto.borrowDate() != null ? "Ngày mượn: " + dto.borrowDate().format(DATE_FMT) : "";
        Span borrowDateSpan = new Span(borrowDateStr);
        borrowDateSpan.addClassNames("text-secondary");
        borrowDateSpan.getStyle().set("font-size", "11px");

        topBar.add(statusBadge, borrowDateSpan);

        // --- 2. MIDDLE: COVER + BOOK INFO ---
        HorizontalLayout bookSection = new HorizontalLayout();
        bookSection.setWidthFull();
        bookSection.setSpacing(true);
        bookSection.setAlignItems(FlexComponent.Alignment.START);

        String coverUrl = (dto.book() != null && dto.book().getCoverImageUrl() != null && !dto.book().getCoverImageUrl().isBlank())
                ? dto.book().getCoverImageUrl() : DEFAULT_COVER;
        Image coverImg = new Image(coverUrl, dto.book() != null ? dto.book().getTitle() : "Bìa sách");
        coverImg.setWidth("75px");
        coverImg.setHeight("105px");
        coverImg.getStyle().set("object-fit", "cover").set("border-radius", "6px").set("box-shadow", "0 2px 6px rgba(0,0,0,0.1)").set("flex-shrink", "0");

        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.setWidthFull();

        String title = dto.book() != null ? dto.book().getTitle() : "Chưa rõ nhan đề";
        Span titleSpan = new Span(title);
        titleSpan.addClassNames("font-bold", "text-primary", "mb-xs");
        titleSpan.getStyle().set("font-size", "14.5px").set("line-height", "1.3");

        String authors = (dto.book() != null && dto.book().getAuthors() != null && !dto.book().getAuthors().isEmpty())
                ? dto.book().getAuthors().stream().map(Author::getFullName).collect(Collectors.joining(", "))
                : "Chưa rõ";
        Span authorSpan = new Span("Tác giả: " + authors);
        authorSpan.addClassNames("text-secondary", "mb-xs");
        authorSpan.getStyle().set("font-size", "12px");

        String categoriesStr = (dto.book() != null && dto.book().getCategories() != null && !dto.book().getCategories().isEmpty())
                ? dto.book().getCategories().stream().map(Category::getName).collect(Collectors.joining(", "))
                : "Chung";
        Span catSpan = new Span("Thể loại: " + categoriesStr);
        catSpan.addClassNames("text-secondary", "mb-xs");
        catSpan.getStyle().set("font-size", "12px");

        info.add(titleSpan, authorSpan, catSpan);
        bookSection.add(coverImg, info);

        // --- 3. HIGHLIGHT INFO BOX ---
        Div highlightBox = new Div();
        highlightBox.setWidthFull();
        highlightBox.addClassNames("rounded-m", "p-s", "my-s", "border");
        if (isOverdue) {
            highlightBox.addClassNames("bg-error-10", "border-error-20");
        } else if (isDueSoon) {
            highlightBox.addClassNames("bg-warning-10", "border-warning-20");
        } else {
            highlightBox.addClassNames("bg-contrast-5", "border-contrast-10");
        }

        String barcode = dto.bookCopy() != null ? dto.bookCopy().getBarcode() : "-";
        Span barcodeSpan = new Span("Mã bản sao: " + barcode);
        barcodeSpan.addClassNames(isOverdue ? "text-error" : "text-primary", "font-medium", "block", "mb-xs");
        barcodeSpan.getStyle().set("font-size", "12px");

        String locName = (dto.bookCopy() != null && dto.bookCopy().getLocation() != null && dto.bookCopy().getLocation().getName() != null)
                ? dto.bookCopy().getLocation().getName() : "Quầy lưu thông thư viện";
        Span locationSpan = new Span("Vị trí: " + locName);
        locationSpan.addClassNames("text-secondary", "font-medium", "block", "mb-xs");
        locationSpan.getStyle().set("font-size", "12px");

        String dueStr = dto.dueDate() != null ? dto.dueDate().format(DATE_FMT) : "-";
        Span dueSpan = new Span("Hạn chót trả sách: " + dueStr);
        dueSpan.addClassNames(isOverdue ? "text-error" : (isDueSoon ? "text-warning" : "text-success"), "font-bold", "block");
        dueSpan.getStyle().set("font-size", "12.5px");

        highlightBox.add(barcodeSpan, locationSpan, dueSpan);

        // --- 4. FOOTER: GIA HẠN INFO & NÚT BẤM ---
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.getStyle().set("margin-top", "8px");

        VerticalLayout renewTextLayout = new VerticalLayout();
        renewTextLayout.setPadding(false);
        renewTextLayout.setSpacing(false);

        Span renewInfo = new Span(String.format("Gia hạn: %d/%d lần", dto.renewCount(), dto.maxRenewals()));
        renewInfo.addClassNames("text-secondary", "font-medium");
        renewInfo.getStyle().set("font-size", "12px");
        renewTextLayout.add(renewInfo);

        if (!dto.canRenew() && dto.cantRenewReason() != null) {
            Span reasonText = new Span(dto.cantRenewReason());
            reasonText.addClassNames("text-error");
            reasonText.getStyle().set("font-size", "11px");
            renewTextLayout.add(reasonText);
        }

        Button renewBtn = new Button("Gia hạn sách", e -> onRenewClick(dto));
        renewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        renewBtn.getStyle().set("border-radius", "6px").set("font-weight", "600");

        if (dto.canRenew()) {
            renewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            renewBtn.setEnabled(false);
            renewBtn.setText("Không thể gia hạn");
        }

        footer.add(renewTextLayout, renewBtn);

        card.add(topBar, bookSection, highlightBox, footer);
        return card;
    }

    private void renderLoansTable(List<ReaderLoanService.CurrentLoanItemDto> loans) {
        Grid<ReaderLoanService.CurrentLoanItemDto> grid = new Grid<>();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.setItems(loans);

        grid.addColumn(new ComponentRenderer<>(dto -> {
            String url = (dto.book() != null && dto.book().getCoverImageUrl() != null
                    && !dto.book().getCoverImageUrl().isBlank())
                            ? dto.book().getCoverImageUrl()
                            : DEFAULT_COVER;
            Image img = new Image(url, "");
            img.setWidth("34px");
            img.setHeight("46px");
            img.getStyle().set("object-fit", "cover").set("border-radius", "3px");
            return img;
        })).setHeader("Bìa").setAutoWidth(true);

        grid.addColumn(dto -> dto.book() != null ? dto.book().getTitle() : "-")
                .setHeader("Nhan đề sách").setAutoWidth(true).setFlexGrow(2);

        grid.addColumn(dto -> dto.bookCopy() != null ? dto.bookCopy().getBarcode() : "-")
                .setHeader("Mã vạch").setAutoWidth(true);

        grid.addColumn(dto -> dto.borrowDate() != null ? dto.borrowDate().format(DATE_FMT) : "-")
                .setHeader("Ngày mượn").setAutoWidth(true);

        grid.addColumn(dto -> dto.dueDate() != null ? dto.dueDate().format(DATE_FMT) : "-")
                .setHeader("Hạn trả").setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(dto -> {
            Span pill = new Span();
            pill.getStyle().set("padding", "2px 8px").set("border-radius", "10px").set("font-size", "11px")
                    .set("font-weight", "600");
            if (dto.isOverdue()) {
                pill.setText(String.format("Quá hạn %d ngày", dto.daysOverdue()));
                pill.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
            } else if (dto.daysRemaining() <= 3) {
                pill.setText(String.format("Còn %d ngày", dto.daysRemaining()));
                pill.getStyle().set("background", "#fef3c7").set("color", "#b45309");
            } else {
                pill.setText(String.format("Còn %d ngày", dto.daysRemaining()));
                pill.getStyle().set("background", "#dcfce7").set("color", "#15803d");
            }
            return pill;
        })).setHeader("Tình trạng").setAutoWidth(true);

        grid.addColumn(dto -> String.format("%d/%d lần", dto.renewCount(), dto.maxRenewals()))
                .setHeader("Đã gia hạn").setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(dto -> {
            Button btn = new Button("Gia hạn", e -> onRenewClick(dto));
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            if (dto.canRenew()) {
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            } else {
                btn.setEnabled(false);
            }
            return btn;
        })).setHeader("Thao tác").setAutoWidth(true);

        tableWrapper.add(grid);
    }

    private void onRenewClick(ReaderLoanService.CurrentLoanItemDto dto) {
        String bookTitle = dto.book() != null ? dto.book().getTitle() : "cuốn sách này";
        int renewDays = (currentMember.getMemberType() != null && currentMember.getMemberType().getRenewDays() != null)
                ? currentMember.getMemberType().getRenewDays()
                : 7;
        String newDue = dto.dueDate() != null ? dto.dueDate().plusDays(renewDays).format(DATE_FMT)
                : "thêm " + renewDays + " ngày";

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Xác nhận gia hạn sách trực tuyến");
        dialog.setText(String.format(
                "Bạn có muốn gia hạn cuốn sách '%s' thêm %d ngày?\nHạn trả mới dự kiến sau khi gia hạn: %s.",
                bookTitle, renewDays, newDue));

        dialog.setCancelable(true);
        dialog.setCancelText("Hủy bỏ");
        dialog.setConfirmText("Đồng ý gia hạn");
        dialog.setConfirmButtonTheme("primary");

        dialog.addConfirmListener(e -> {
            try {
                readerLoanService.renewOnline(dto.loanItem().getId());
                toastService.success(String.format("Gia hạn thành công! Hạn trả mới: %s", newDue));
                reloadData();
            } catch (Exception ex) {
                toastService.error("Không thể gia hạn: " + ex.getMessage());
            }
        });

        dialog.open();
    }
}
