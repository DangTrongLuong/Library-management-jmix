package com.company.librarymanagementsystem.view.librarian;

import com.company.librarymanagementsystem.entity.User;
import com.company.librarymanagementsystem.service.LibrarianShiftService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.fine.FineListView;
import com.company.librarymanagementsystem.view.loan.LoanCreateView;
import com.company.librarymanagementsystem.view.loan.LoanListView;
import com.company.librarymanagementsystem.view.main.MainView;
import com.company.librarymanagementsystem.view.member.MemberListView;
import com.company.librarymanagementsystem.view.reservation.ReservationListView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.download.ByteArrayDownloadDataProvider;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Route(value = "librarian-shift-dashboard", layout = MainView.class)
@ViewController("LibrarianShiftDashboardView")
@ViewDescriptor("librarian-shift-dashboard-view.xml")
public class LibrarianShiftDashboardView extends StandardView {

    @ViewComponent
    private DatePicker shiftDatePicker;
    @ViewComponent
    private Button refreshBtn;
    @ViewComponent
    private Button autoMatchBtn;
    @ViewComponent
    private Button cleanExpiredBtn;
    @ViewComponent
    private Button exportHandoverBtn;

    @ViewComponent
    private Button navNewLoanBtn;
    @ViewComponent
    private Button navReturnBooksBtn;
    @ViewComponent
    private Button navCollectFineBtn;
    @ViewComponent
    private Button navReservationBtn;
    @ViewComponent
    private Button navRegisterMemberBtn;

    @ViewComponent
    private Div kpiCardsContainer;
    @ViewComponent
    private Div readyReservationsContainer;
    @ViewComponent
    private Div bottomGridContainer;

    @Autowired
    private LibrarianShiftService shiftService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private Downloader downloader;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter fullDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Subscribe
    public void onInit(final InitEvent event) {
        getContent().setWidthFull();
        getContent().setMaxWidth("100%");
        getContent().getStyle().set("box-sizing", "border-box").set("overflow-x", "hidden");

        if (shiftDatePicker != null) {
            shiftDatePicker.setValue(LocalDate.now());
            shiftDatePicker.addValueChangeListener(e -> loadDashboardData());
        }

        initButtons();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        loadDashboardData();
    }

    private void initButtons() {
        refreshBtn.addClickListener(e -> {
            loadDashboardData();
            toastService.success("Đã làm mới dữ liệu ca trực!");
        });

        autoMatchBtn.addClickListener(e -> {
            try {
                int matched = shiftService.autoMatchPendingReservations();
                loadDashboardData();
                if (matched > 0) {
                    toastService.success(String.format("Đã tự động gán %d bản sao khả dụng cho các lượt đặt trước!", matched));
                } else {
                    toastService.info("Không có bản sao khả dụng nào khớp với lượt chờ hiện tại.");
                }
            } catch (Exception ex) {
                toastService.error("Lỗi khi điều phối sách: " + ex.getMessage());
            }
        });

        cleanExpiredBtn.addClickListener(e -> {
            try {
                int processed = shiftService.processExpiredReservations();
                loadDashboardData();
                if (processed > 0) {
                    toastService.success(String.format("Đã giải phóng %d lượt đặt trước quá hạn giữ chỗ!", processed));
                } else {
                    toastService.info("Không có lượt đặt trước nào quá hạn cần giải phóng.");
                }
            } catch (Exception ex) {
                toastService.error("Lỗi khi quét hạn đặt trước: " + ex.getMessage());
            }
        });

        exportHandoverBtn.addClickListener(this::onExportHandoverReportClick);

        // Quick navigation shortcuts
        navNewLoanBtn.addClickListener(e -> viewNavigators.view(this, LoanCreateView.class).navigate());
        navReturnBooksBtn.addClickListener(e -> viewNavigators.view(this, LoanListView.class).navigate());
        navCollectFineBtn.addClickListener(e -> viewNavigators.view(this, FineListView.class).navigate());
        navReservationBtn.addClickListener(e -> viewNavigators.view(this, ReservationListView.class).navigate());
        navRegisterMemberBtn.addClickListener(e -> viewNavigators.view(this, MemberListView.class).navigate());
    }

    private LocalDate getSelectedDate() {
        if (shiftDatePicker != null && shiftDatePicker.getValue() != null) {
            return shiftDatePicker.getValue();
        }
        return LocalDate.now();
    }

    private void loadDashboardData() {
        LocalDate date = getSelectedDate();
        LibrarianShiftService.ShiftStats stats = shiftService.getShiftStats(date);
        List<LibrarianShiftService.ReadyReservationItem> readyReservations = shiftService.getReadyReservations();
        List<LibrarianShiftService.ShiftActivityItem> activities = shiftService.getRecentShiftActivities(date, 15);
        List<LibrarianShiftService.UrgentOverdueItem> overdueList = shiftService.getUrgentOverdueList(10);

        renderKpiCards(stats);
        renderReadyReservations(readyReservations);
        renderBottomGrid(activities, overdueList);
    }

    /**
     * Hiển thị 4 thẻ KPI thống kê trọng tâm theo ca và theo ngày
     */
    private void renderKpiCards(LibrarianShiftService.ShiftStats stats) {
        kpiCardsContainer.removeAll();

        Div grid = new Div();
        grid.addClassNames("grid", "grid-cols-2", "lg:grid-cols-4", "gap-s", "w-full");
        grid.getStyle()
                .set("box-sizing", "border-box")
                .set("max-width", "100%")
                .set("min-width", "0");

        // Thẻ 1: Lượt mượn trong ngày (Tông xanh dương pastel)
        grid.add(createKpiCard(
                "Lượt Mượn Trong Ngày",
                String.format("%,d lượt", stats.loansCount()),
                String.format("%,d cuốn sách cho mượn", stats.loanCopiesCount()),
                "#f0f9ff",
                "#bae6fd",
                "#0284c7",
                "#0369a1",
                VaadinIcon.ARROW_UP
        ));

        // Thẻ 2: Lượt trả trong ngày (Tông xanh ngọc pastel)
        grid.add(createKpiCard(
                "Lượt Trả Trong Ngày",
                String.format("%,d cuốn", stats.returnsCount()),
                String.format("%,d nguyên vẹn | %,d phát sinh hỏng", stats.returnsGoodCount(), stats.returnsDamagedOrLostCount()),
                "#f0fdf4",
                "#bbf7d0",
                "#16a34a",
                "#15803d",
                VaadinIcon.ARROW_DOWN
        ));

        // Thẻ 3: Sách đặt trước chờ lấy tại quầy (Tông hổ phách pastel)
        String resSub = stats.readyExpiringTodayCount() > 0
                ? String.format("Có %,d cuốn sắp hết hạn hôm nay!", stats.readyExpiringTodayCount())
                : "Bản sao sẵn sàng giữ tại quầy";
        grid.add(createKpiCard(
                "Sách Chờ Lấy Tại Quầy",
                String.format("%,d cuốn", stats.readyReservationsCount()),
                resSub,
                stats.readyExpiringTodayCount() > 0 ? "#fff1f2" : "#fffbeb",
                stats.readyExpiringTodayCount() > 0 ? "#fecdd3" : "#fde68a",
                stats.readyExpiringTodayCount() > 0 ? "#e11d48" : "#d97706",
                stats.readyExpiringTodayCount() > 0 ? "#be123c" : "#b45309",
                VaadinIcon.BOOKMARK
        ));

        // Thẻ 4: Tiền phạt thu được trong ca (Tông tím pastel)
        grid.add(createKpiCard(
                "Tiền Phạt Thu Trong Ca",
                String.format("%s đ", currencyFormat.format(stats.finesCollected())),
                String.format("%,d biên lai | %,d độc giả ghé quầy", stats.paymentsCount(), stats.activePatronsVisited()),
                "#faf5ff",
                "#e9d5ff",
                "#9333ea",
                "#7e22ce",
                VaadinIcon.MONEY
        ));

        kpiCardsContainer.add(grid);
    }

    private Div createKpiCard(String title, String value, String subtext,
                              String cardBg, String borderColor, String iconColor, String titleColor,
                              VaadinIcon icon) {
        Div card = new Div();
        card.addClassNames("rounded-m", "p-s", "border", "shadow-xs", "flex", "flex-col", "justify-between");
        card.getStyle()
                .set("background", cardBg)
                .set("border-color", borderColor)
                .set("box-sizing", "border-box")
                .set("min-width", "0")
                .set("min-height", "84px")
                .set("position", "relative")
                .set("overflow", "hidden");

        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);
        topRow.setSpacing(false);
        topRow.setPadding(false);

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", titleColor)
                .set("letter-spacing", "0.2px");

        Div iconBox = new Div();
        iconBox.getStyle()
                .set("width", "26px")
                .set("height", "26px")
                .set("border-radius", "6px")
                .set("background", "rgba(255, 255, 255, 0.7)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("border", "1px solid " + borderColor)
                .set("flex-shrink", "0");

        var iconComp = icon.create();
        iconComp.getStyle().set("width", "14px").set("height", "14px").set("color", iconColor);
        iconBox.add(iconComp);

        topRow.add(titleSpan, iconBox);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-family", "var(--lumo-font-family, 'Plus Jakarta Sans', 'Inter', sans-serif)")
                .set("font-size", "20px")
                .set("font-weight", "800")
                .set("color", "#0f172a")
                .set("margin", "2px 0");

        Span subSpan = new Span(subtext);
        subSpan.getStyle()
                .set("font-family", "var(--lumo-font-family, 'Plus Jakarta Sans', 'Inter', sans-serif)")
                .set("font-size", "11px")
                .set("color", "#64748b")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        card.add(topRow, valueSpan, subSpan);
        return card;
    }

    /**
     * Bảng danh sách sách đặt trước đang chờ độc giả ghé lấy tại quầy (READY)
     */
    private void renderReadyReservations(List<LibrarianShiftService.ReadyReservationItem> items) {
        readyReservationsContainer.removeAll();

        Div sectionBox = new Div();
        sectionBox.addClassNames("bg-base", "p-m", "rounded-l", "border", "border-contrast-10", "shadow-xs", "flex", "flex-col");
        sectionBox.getStyle()
                .set("box-sizing", "border-box")
                .set("max-width", "100%")
                .set("min-width", "0");

        // Header section
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout titleLeft = new HorizontalLayout();
        titleLeft.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLeft.setSpacing(true);

        H4 titleHeader = new H4("Sách Đặt Trước Đang Chờ Độc Giả Ghé Lấy Tại Quầy (READY)");
        titleHeader.addClassNames("m-0", "text-m", "font-bold", "text-primary");

        Span countBadge = new Span(items.size() + " cuốn");
        countBadge.addClassNames("badge", items.isEmpty() ? "contrast" : "warning", "small");

        titleLeft.add(titleHeader, countBadge);

        Span noteSub = new Span("Giữ sách tối đa 3 ngày theo quy định. Thủ thư có thể lập phiếu mượn 1 chạm khi độc giả đến nhận.");
        noteSub.addClassNames("text-secondary", "text-xs");

        headerRow.add(titleLeft);
        sectionBox.add(headerRow, noteSub);

        // Content
        if (items.isEmpty()) {
            Div emptyBox = new Div();
            emptyBox.addClassNames("p-l", "text-center", "text-secondary", "bg-contrast-5", "rounded-m", "mt-s");
            emptyBox.add(new Span("Hiện tại không có cuốn sách nào đang chờ nhận tại quầy. Tất cả bản sao đã được bàn giao hoặc kệ sách trống."));
            sectionBox.add(emptyBox);
        } else {
            Div tableContainer = new Div();
            tableContainer.addClassNames("flex", "flex-col", "gap-xs", "mt-s");
            tableContainer.getStyle()
                    .set("overflow-x", "auto")
                    .set("max-width", "100%");

            // Table header
            Div tableHeader = new Div();
            tableHeader.addClassNames("grid", "grid-cols-12", "gap-s", "p-s", "bg-contrast-5", "rounded-m", "text-secondary", "text-xs", "font-bold", "uppercase");
            tableHeader.getStyle().set("min-width", "820px");

            Span thBook = new Span("Đầu Sách & ISBN");
            thBook.getStyle().set("grid-column", "span 3");

            Span thBarcode = new Span("Mã Vạch Giữ Chỗ");
            thBarcode.getStyle().set("grid-column", "span 2");

            Span thMember = new Span("Độc Giả Nhận Sách");
            thMember.getStyle().set("grid-column", "span 3");

            Span thExpires = new Span("Hạn Giữ Tại Quầy");
            thExpires.getStyle().set("grid-column", "span 2");

            Span thAction = new Span("Tác Vụ Quầy");
            thAction.getStyle().set("grid-column", "span 2").set("text-align", "right");

            tableHeader.add(thBook, thBarcode, thMember, thExpires, thAction);
            tableContainer.add(tableHeader);

            // Table rows
            for (LibrarianShiftService.ReadyReservationItem item : items) {
                Div row = new Div();
                row.addClassNames("grid", "grid-cols-12", "gap-s", "p-s", "border-b", "border-contrast-10", "items-center", "hover:bg-contrast-5");
                row.getStyle().set("min-width", "820px");

                // Book info
                VerticalLayout bookCol = new VerticalLayout();
                bookCol.setPadding(false);
                bookCol.setSpacing(false);
                bookCol.getStyle().set("grid-column", "span 3");

                Span bookTitle = new Span(item.bookTitle());
                bookTitle.getStyle().set("font-weight", "600").set("font-size", "13px").set("color", "#0f172a");

                Span bookIsbn = new Span("ISBN: " + item.bookIsbn());
                bookIsbn.addClassNames("text-secondary", "text-xs");
                bookCol.add(bookTitle, bookIsbn);

                // Barcode
                Div barcodeCol = new Div();
                barcodeCol.getStyle().set("grid-column", "span 2");
                Span barcodeBadge = new Span(item.copyBarcode());
                barcodeBadge.getStyle()
                        .set("font-family", "monospace")
                        .set("background", "#e0f2fe")
                        .set("color", "#0369a1")
                        .set("padding", "3px 8px")
                        .set("border-radius", "4px")
                        .set("font-size", "12px")
                        .set("font-weight", "700");
                barcodeCol.add(barcodeBadge);

                // Member info
                VerticalLayout memberCol = new VerticalLayout();
                memberCol.setPadding(false);
                memberCol.setSpacing(false);
                memberCol.getStyle().set("grid-column", "span 3");

                Span memberName = new Span(item.memberName() + " (" + item.memberCode() + ")");
                memberName.getStyle().set("font-weight", "600").set("font-size", "13px");

                Span memberPhone = new Span("SĐT: " + item.memberPhone());
                memberPhone.addClassNames("text-secondary", "text-xs");
                memberCol.add(memberName, memberPhone);

                // Hold expires
                VerticalLayout expiresCol = new VerticalLayout();
                expiresCol.setPadding(false);
                expiresCol.setSpacing(false);
                expiresCol.getStyle().set("grid-column", "span 2");

                String expiresStr = item.holdExpiresAt() != null ? item.holdExpiresAt().format(fullDateTimeFormatter) : "-";
                Span expiresTime = new Span(expiresStr);
                expiresTime.getStyle().set("font-size", "12px");

                Span statusTag = new Span(item.statusBadge());
                statusTag.addClassNames("badge", item.badgeTheme(), "small");
                statusTag.getStyle().set("width", "fit-content").set("margin-top", "2px");

                expiresCol.add(expiresTime, statusTag);

                // Actions
                HorizontalLayout actionCol = new HorizontalLayout();
                actionCol.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                actionCol.setSpacing(true);
                actionCol.getStyle().set("grid-column", "span 2");

                Button loanBtn = new Button("Mượn ngay", VaadinIcon.PLUS_CIRCLE.create(), e -> onDirectLoanClick(item));
                loanBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                loanBtn.setTooltipText("Lập phiếu mượn nhanh cho độc giả đã đến nhận sách");

                Button cancelBtn = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> onCancelReservationClick(item));
                cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                cancelBtn.setTooltipText("Hủy giữ chỗ và trả bản sao về kệ hoặc điều phối người tiếp theo");

                actionCol.add(loanBtn, cancelBtn);

                row.add(bookCol, barcodeCol, memberCol, expiresCol, actionCol);
                tableContainer.add(row);
            }

            sectionBox.add(tableContainer);
        }

        readyReservationsContainer.add(sectionBox);
    }

    /**
     * Lập phiếu mượn trực tiếp 1-chạm từ sách đặt trước sẵn sàng
     */
    private void onDirectLoanClick(LibrarianShiftService.ReadyReservationItem item) {
        if (item == null) return;

        Map<String, List<String>> params = new HashMap<>();
        if (item.memberId() != null) {
            params.put("memberId", List.of(item.memberId().toString()));
        }
        if (item.copyBarcode() != null && !item.copyBarcode().isBlank() && !"-".equals(item.copyBarcode())) {
            params.put("barcode", List.of(item.copyBarcode()));
        }

        viewNavigators.view(this, LoanCreateView.class)
                .withQueryParameters(new QueryParameters(params))
                .navigate();
    }

    /**
     * Hủy yêu cầu đặt trước tại quầy
     */
    private void onCancelReservationClick(LibrarianShiftService.ReadyReservationItem item) {
        if (item == null) return;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Hủy Giữ Chỗ Sách Tại Quầy");
        dialog.setWidth("30em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        Span info = new Span(String.format("Bạn có chắc chắn muốn hủy giữ bản sao '%s' cuốn '%s' cho độc giả %s?",
                item.copyBarcode(), item.bookTitle(), item.memberName()));
        info.getStyle().set("font-size", "13px");

        TextField reasonField = new TextField("Lý do hủy");
        reasonField.setWidthFull();
        reasonField.setValue("Độc giả không đến nhận tại quầy / Đã hết thời hạn giữ");

        layout.add(info, reasonField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận hủy", e -> {
            try {
                shiftService.cancelReservation(item.reservationId(), reasonField.getValue());
                toastService.success("Đã hủy giữ chỗ và trả bản sao về lưu thông thành công!");
                dialog.close();
                loadDashboardData();
            } catch (Exception ex) {
                toastService.error("Lỗi khi hủy: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button closeBtn = new Button("Đóng", e -> dialog.close());

        dialog.getFooter().add(closeBtn, confirmBtn);
        dialog.open();
    }

    /**
     * Cột trái: Feed hoạt động giao dịch ca trực. Cột phải: Cảnh báo sách quá hạn nghiêm trọng
     */
    private void renderBottomGrid(List<LibrarianShiftService.ShiftActivityItem> activities,
                                  List<LibrarianShiftService.UrgentOverdueItem> overdueList) {
        bottomGridContainer.removeAll();

        // 1. Box Hoạt động trong ca (Shift Activity Feed)
        Div activityBox = new Div();
        activityBox.addClassNames("bg-base", "p-m", "rounded-l", "border", "border-contrast-10", "shadow-xs", "flex", "flex-col");
        activityBox.getStyle()
                .set("box-sizing", "border-box")
                .set("max-width", "100%")
                .set("min-width", "0");

        HorizontalLayout actHeaderRow = new HorizontalLayout();
        actHeaderRow.setWidthFull();
        actHeaderRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        actHeaderRow.setAlignItems(FlexComponent.Alignment.CENTER);

        H4 actTitle = new H4("Nhật Ký Giao Dịch Trong Ca");
        actTitle.addClassNames("m-0", "text-m", "font-bold", "text-primary");

        Span actBadge = new Span(activities.size() + " sự kiện");
        actBadge.addClassNames("badge", "primary", "small");

        actHeaderRow.add(actTitle, actBadge);
        activityBox.add(actHeaderRow);

        if (activities.isEmpty()) {
            Div emptyBox = new Div();
            emptyBox.addClassNames("p-l", "text-center", "text-secondary", "bg-contrast-5", "rounded-m", "mt-s");
            emptyBox.add(new Span("Chưa ghi nhận giao dịch mượn/trả/thu phạt nào trong ca của ngày này."));
            activityBox.add(emptyBox);
        } else {
            Div listContainer = new Div();
            listContainer.addClassNames("flex", "flex-col", "gap-s", "mt-s");
            listContainer.getStyle()
                    .set("max-height", "420px")
                    .set("overflow-y", "auto");

            for (LibrarianShiftService.ShiftActivityItem act : activities) {
                HorizontalLayout itemRow = new HorizontalLayout();
                itemRow.setWidthFull();
                itemRow.addClassNames("p-xs", "border-b", "border-contrast-10", "items-center", "gap-s");

                // Badge time
                Span timeBadge = new Span(act.time() != null ? act.time().format(timeFormatter) : "--:--");
                timeBadge.getStyle()
                        .set("font-family", "monospace")
                        .set("font-size", "12px")
                        .set("color", "#64748b")
                        .set("min-width", "42px");

                // Badge type
                Span typeTag = new Span(act.typeLabel());
                typeTag.addClassNames("badge", act.badgeTheme(), "small");
                typeTag.getStyle().set("min-width", "72px").set("text-align", "center");

                // Info
                VerticalLayout infoCol = new VerticalLayout();
                infoCol.setPadding(false);
                infoCol.setSpacing(false);
                infoCol.getStyle().set("flex-grow", "1").set("min-width", "0");

                Span titleText = new Span(act.title());
                titleText.getStyle().set("font-weight", "600").set("font-size", "13px").set("color", "#0f172a");

                Span descText = new Span(act.description());
                descText.addClassNames("text-secondary", "text-xs");
                descText.getStyle()
                        .set("white-space", "nowrap")
                        .set("overflow", "hidden")
                        .set("text-overflow", "ellipsis");

                infoCol.add(titleText, descText);

                // Staff
                Span staffText = new Span(act.staffName());
                staffText.addClassNames("text-secondary", "text-xs");
                staffText.getStyle().set("min-width", "80px").set("text-align", "right");

                itemRow.add(timeBadge, typeTag, infoCol, staffText);
                listContainer.add(itemRow);
            }
            activityBox.add(listContainer);
        }

        // 2. Box Sách quá hạn cần nhắc độc giả tại quầy
        Div overdueBox = new Div();
        overdueBox.addClassNames("bg-base", "p-m", "rounded-l", "border", "border-contrast-10", "shadow-xs", "flex", "flex-col");
        overdueBox.getStyle()
                .set("box-sizing", "border-box")
                .set("max-width", "100%")
                .set("min-width", "0");

        HorizontalLayout ovHeaderRow = new HorizontalLayout();
        ovHeaderRow.setWidthFull();
        ovHeaderRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        ovHeaderRow.setAlignItems(FlexComponent.Alignment.CENTER);

        H4 ovTitle = new H4("Sách Quá Hạn Cần Lưu Ý Khi Giao Dịch Tại Quầy");
        ovTitle.addClassNames("m-0", "text-m", "font-bold", "text-primary");

        Span ovBadge = new Span(overdueList.size() + " cuốn");
        ovBadge.addClassNames("badge", overdueList.isEmpty() ? "success" : "error", "small");

        ovHeaderRow.add(ovTitle, ovBadge);
        overdueBox.add(ovHeaderRow);

        if (overdueList.isEmpty()) {
            Div emptyBox = new Div();
            emptyBox.addClassNames("p-l", "text-center", "text-secondary", "bg-contrast-5", "rounded-m", "mt-s");
            emptyBox.add(new Span("Không có độc giả nào quá hạn nghiêm trọng cần nhắc nhở tại quầy."));
            overdueBox.add(emptyBox);
        } else {
            Div overdueListLayout = new Div();
            overdueListLayout.addClassNames("flex", "flex-col", "gap-xs", "mt-s");
            overdueListLayout.getStyle()
                    .set("max-height", "420px")
                    .set("overflow-y", "auto");

            for (LibrarianShiftService.UrgentOverdueItem item : overdueList) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.addClassNames("p-xs", "border-b", "border-contrast-10", "items-center", "gap-s");

                var warnIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
                warnIcon.getStyle().set("width", "16px").set("height", "16px").set("color", "#e11d48").set("flex-shrink", "0");

                VerticalLayout infoCol = new VerticalLayout();
                infoCol.setPadding(false);
                infoCol.setSpacing(false);
                infoCol.getStyle().set("flex-grow", "1").set("min-width", "0");

                Span bookTitleSpan = new Span(item.bookTitle());
                bookTitleSpan.getStyle().set("font-weight", "600").set("font-size", "13px").set("color", "#0f172a");

                Span readerSub = new Span(String.format("Mã vạch: %s | Độc giả: %s (SĐT: %s)",
                        item.copyBarcode(), item.memberName(), item.memberPhone()));
                readerSub.addClassNames("text-secondary", "text-xs");

                infoCol.add(bookTitleSpan, readerSub);

                VerticalLayout fineCol = new VerticalLayout();
                fineCol.setPadding(false);
                fineCol.setSpacing(false);
                fineCol.setAlignItems(FlexComponent.Alignment.END);
                fineCol.getStyle().set("min-width", "90px");

                Span daysBadge = new Span("Quá " + item.daysOverdue() + " ngày");
                daysBadge.addClassNames("badge", "error", "small");

                Span fineEst = new Span("~ " + currencyFormat.format(item.estimatedFine()) + " đ");
                fineEst.addClassNames("text-secondary", "text-xs");

                fineCol.add(daysBadge, fineEst);

                row.add(warnIcon, infoCol, fineCol);
                overdueListLayout.add(row);
            }
            overdueBox.add(overdueListLayout);
        }

        bottomGridContainer.add(activityBox, overdueBox);
    }

    /**
     * Xuất biên bản bàn giao ca trực dạng CSV chuẩn UTF-8
     */
    private void onExportHandoverReportClick(ClickEvent<Button> event) {
        LocalDate date = getSelectedDate();
        LibrarianShiftService.ShiftStats stats = shiftService.getShiftStats(date);
        List<LibrarianShiftService.ReadyReservationItem> readyReservations = shiftService.getReadyReservations();
        List<LibrarianShiftService.ShiftActivityItem> activities = shiftService.getRecentShiftActivities(date, 50);

        String currentStaff = "Thủ thư trực ca";
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User u) {
            currentStaff = u.getFullName() != null ? u.getFullName() : u.getUsername();
        }

        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM
        csv.append("\uFEFF");
        csv.append("BIÊN BẢN BÀN GIAO CA TRỰC THỦ THƯ\n");
        csv.append("Ngày ca trực:,").append(date.format(dateFormatter)).append("\n");
        csv.append("Thời gian xuất báo cáo:,").append(LocalDate.now().format(dateFormatter)).append("\n");
        csv.append("Người lập biên bản:,").append(currentStaff).append("\n\n");

        csv.append("--- TỔNG KẾT CHỈ SỐ CA TRỰC ---\n");
        csv.append("Số lượt mượn trong ca:,").append(stats.loansCount()).append(" lượt\n");
        csv.append("Tổng số cuốn sách đã cho mượn:,").append(stats.loanCopiesCount()).append(" cuốn\n");
        csv.append("Số cuốn sách đã nhận trả:,").append(stats.returnsCount()).append(" cuốn\n");
        csv.append("Trong đó trả sách nguyên vẹn:,").append(stats.returnsGoodCount()).append(" cuốn\n");
        csv.append("Trong đó trả sách hỏng/mất (lập phạt):,").append(stats.returnsDamagedOrLostCount()).append(" cuốn\n");
        csv.append("Số sách đặt trước đang chờ độc giả tại quầy:,").append(stats.readyReservationsCount()).append(" cuốn\n");
        csv.append("Số sách chờ nhận sắp hết hạn trong ngày:,").append(stats.readyExpiringTodayCount()).append(" cuốn\n");
        csv.append("Tổng tiền phạt thu được trong ca trực:,").append(currencyFormat.format(stats.finesCollected())).append(" VNĐ\n");
        csv.append("Số lượng biên lai thu phạt:,").append(stats.paymentsCount()).append(" biên lai\n");
        csv.append("Số lượt gia hạn sách đã xử lý:,").append(stats.renewalsCount()).append(" lượt\n");
        csv.append("Số độc giả ghé quầy giao dịch:,").append(stats.activePatronsVisited()).append(" độc giả\n\n");

        csv.append("--- DANH SÁCH SÁCH ĐẶT TRƯỚC ĐANG GIỮ TẠI QUẦY (READY) ---\n");
        csv.append("Mã Đặt Trước,Tên Đầu Sách,ISBN,Mã Vạch Giữ Chỗ,Tên Độc Giả,Mã Thẻ,Số Điện Thoại,Hạn Giữ Chỗ,Trạng Thái\n");
        for (LibrarianShiftService.ReadyReservationItem r : readyReservations) {
            csv.append(r.reservationId()).append(",")
                    .append("\"").append(r.bookTitle().replace("\"", "\"\"")).append("\",")
                    .append(r.bookIsbn()).append(",")
                    .append(r.copyBarcode()).append(",")
                    .append("\"").append(r.memberName().replace("\"", "\"\"")).append("\",")
                    .append(r.memberCode()).append(",")
                    .append(r.memberPhone()).append(",")
                    .append(r.holdExpiresAt() != null ? r.holdExpiresAt().format(fullDateTimeFormatter) : "-").append(",")
                    .append(r.statusBadge()).append("\n");
        }
        csv.append("\n");

        csv.append("--- NHẬT KÝ GIAO DỊCH TRONG CA TRỰC ---\n");
        csv.append("Thời Gian,Nghiệp Vụ,Tiêu Đề,Nội Dung Chi Tiết,Độc Giả,Nhân Viên Xử Lý\n");
        for (LibrarianShiftService.ShiftActivityItem act : activities) {
            csv.append(act.time() != null ? act.time().format(fullDateTimeFormatter) : "-").append(",")
                    .append(act.typeLabel()).append(",")
                    .append("\"").append(act.title().replace("\"", "\"\"")).append("\",")
                    .append("\"").append(act.description().replace("\"", "\"\"")).append("\",")
                    .append("\"").append(act.memberName().replace("\"", "\"\"")).append("\",")
                    .append("\"").append(act.staffName().replace("\"", "\"\"")).append("\"\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        downloader.download(new ByteArrayDownloadDataProvider(bytes, 8192, "UTF-8"),
                "BienBan_CaTruc_ThuVien_" + date + ".csv");
        toastService.success("Đã xuất biên bản bàn giao ca trực thành công!");
    }
}
