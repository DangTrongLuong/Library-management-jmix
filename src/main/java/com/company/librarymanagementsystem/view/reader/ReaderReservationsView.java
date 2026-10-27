package com.company.librarymanagementsystem.view.reader;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.ReaderLoanService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.company.librarymanagementsystem.view.opac.OpacCatalogView;
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
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "reader/my-reservations", layout = MainView.class)
@ViewController(id = "ReaderReservationsView")
@ViewDescriptor(path = "reader-reservations-view.xml")
public class ReaderReservationsView extends StandardView {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String DEFAULT_COVER = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=300&auto=format&fit=crop&q=80";

    @ViewComponent
    private Button viewModeActiveBtn;
    @ViewComponent
    private Button viewModeHistoryBtn;
    @ViewComponent
    private Button exploreBooksBtn;
    @ViewComponent
    private Button refreshBtn;

    @ViewComponent
    private Div kpiContainer;
    @ViewComponent
    private VerticalLayout activeContainer;
    @ViewComponent
    private Div activeCardsGrid;
    @ViewComponent
    private Div emptyActiveState;

    @ViewComponent
    private VerticalLayout historyContainer;
    @ViewComponent
    private Div historyGridWrapper;
    @ViewComponent
    private Div emptyHistoryState;

    @Autowired
    private ReaderLoanService readerLoanService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private ViewNavigators viewNavigators;

    private Member currentMember;
    private boolean isActiveMode = true;

    @Subscribe
    public void onInit(InitEvent event) {
        viewModeActiveBtn.addClickListener(e -> switchTab(true));
        viewModeHistoryBtn.addClickListener(e -> switchTab(false));
        exploreBooksBtn.addClickListener(e -> viewNavigators.view(this, OpacCatalogView.class).navigate());
        refreshBtn.addClickListener(e -> reloadData());
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        reloadData();
    }

    private void switchTab(boolean activeMode) {
        this.isActiveMode = activeMode;
        if (activeMode) {
            viewModeActiveBtn.setThemeName("primary small");
            viewModeHistoryBtn.setThemeName("tertiary small");
            activeContainer.setVisible(true);
            historyContainer.setVisible(false);
        } else {
            viewModeActiveBtn.setThemeName("tertiary small");
            viewModeHistoryBtn.setThemeName("primary small");
            activeContainer.setVisible(false);
            historyContainer.setVisible(true);
        }
    }

    private void reloadData() {
        Optional<Member> memberOpt = readerLoanService.getCurrentMember();
        if (memberOpt.isEmpty()) {
            renderNoMemberState();
            return;
        }

        this.currentMember = memberOpt.get();
        renderKpis();
        renderActiveReservations();
        renderHistoryReservations();
    }

    private void renderNoMemberState() {
        kpiContainer.removeAll();
        activeCardsGrid.removeAll();
        historyGridWrapper.removeAll();

        Div alert = new Div();
        alert.addClassNames("bg-warning-10", "border", "border-warning-30", "rounded-m", "p-m", "text-center", "box-border", "flex", "flex-col", "items-center", "gap-s");
        Span msg = new Span("Tài khoản của bạn chưa được liên kết với hồ sơ độc giả thư viện. Nếu bạn đã có thẻ làm tại quầy, hãy liên kết thẻ để bắt đầu đặt trước sách.");
        msg.addClassNames("text-warning", "font-medium", "text-s");
        
        Button linkBtn = new Button("Đi đến trang Liên kết Thẻ Thư Viện", VaadinIcon.LINK.create(), e -> UI.getCurrent().navigate("reader-profile"));
        linkBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        alert.add(msg, linkBtn);
        kpiContainer.add(alert);
    }

    private void renderKpis() {
        kpiContainer.removeAll();
        ReaderLoanService.ReservationSummaryDto stats = readerLoanService.getReservationSummary(currentMember);

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        // 1. Đang chờ sách (PENDING)
        grid.add(createKpiCard(
                "Đang Chờ Sách",
                String.valueOf(stats.pendingCount()),
                "Trong hàng đợi chờ bản sao",
                "bg-warning-10 text-warning border-warning-20",
                "text-warning"
        ));

        // 2. Sẵn sàng nhận (READY)
        grid.add(createKpiCard(
                "Sẵn Sàng Nhận",
                String.valueOf(stats.readyCount()),
                "Đã giữ tại quầy lưu thông",
                stats.readyCount() > 0 ? "bg-success-10 text-success border-success-30" : "bg-base text-secondary border-contrast-10",
                stats.readyCount() > 0 ? "text-success font-bold" : "text-primary"
        ));

        // 3. Lịch sử đặt trước
        grid.add(createKpiCard(
                "Lịch Sử Hoàn Tất",
                String.valueOf(stats.historyCount()),
                "Lượt mượn / hủy trước đây",
                "bg-base text-secondary border-contrast-10",
                "text-primary"
        ));

        // 4. Hạn mức đặt trước
        int activeCount = stats.pendingCount() + stats.readyCount();
        grid.add(createKpiCard(
                "Hạn Mức Đang Dùng",
                activeCount + " / " + stats.maxReservations(),
                "Tối đa " + stats.maxReservations() + " cuốn cùng lúc",
                "bg-base text-secondary border-contrast-10",
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

    private void renderActiveReservations() {
        activeCardsGrid.removeAll();
        emptyActiveState.removeAll();

        List<ReaderLoanService.ReaderReservationDto> list = readerLoanService.getActiveReservations(currentMember.getId());

        if (list.isEmpty()) {
            emptyActiveState.setVisible(true);
            activeCardsGrid.setVisible(false);

            Div emptyBox = new Div();
            emptyBox.setWidthFull();
            emptyBox.addClassNames("bg-base", "rounded-l", "border", "border-contrast-10", "p-xl", "text-center", "shadow-s", "box-border");
            emptyBox.getStyle().set("box-sizing", "border-box").set("max-width", "100%");

            var icon = VaadinIcon.BOOK.create();
            icon.getStyle().set("font-size", "2.5rem").set("color", "var(--lumo-primary-color)").set("margin-bottom", "0.5rem");

            H3 title = new H3("Bạn hiện không có yêu cầu đặt trước sách nào");
            title.addClassNames("font-bold", "text-primary", "mb-xs");
            title.getStyle().set("font-size", "15px");

            Paragraph desc = new Paragraph("Khi những cuốn sách bạn yêu thích đang được người khác mượn hết, hãy chọn Đặt trước tại mục Tra cứu sách để được tự động xếp hàng giữ chỗ ngay khi có bản sao hoàn trả!");
            desc.addClassNames("text-secondary", "max-w-md", "mx-auto", "mb-m");
            desc.getStyle().set("font-size", "12.5px");

            Button searchBtn = new Button("Khám phá kho sách & Đặt trước", e ->
                    viewNavigators.view(this, OpacCatalogView.class).navigate());
            searchBtn.setThemeName("primary small");

            emptyBox.add(icon, title, desc, searchBtn);
            emptyActiveState.add(emptyBox);
            return;
        }

        emptyActiveState.setVisible(false);
        activeCardsGrid.setVisible(true);

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(380px, 1fr))")
                .set("gap", "16px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        for (ReaderLoanService.ReaderReservationDto dto : list) {
            grid.add(createActiveReservationCard(dto));
        }

        activeCardsGrid.add(grid);
    }

    private Div createActiveReservationCard(ReaderLoanService.ReaderReservationDto dto) {
        Reservation res = dto.reservation();
        Book book = res.getBook();
        BookCopy copy = res.getBookCopy();
        boolean isReady = ReservationStatus.READY.getId().equalsIgnoreCase(res.getStatus());

        Div card = new Div();
        card.addClassNames("bg-base", "rounded-l", "border", "shadow-s", "p-m", "flex", "flex-col", "justify-between", "transition-all");
        card.getStyle().set("border-color", isReady ? "var(--lumo-success-color-50pct, #22c55e)" : "var(--lumo-contrast-15pct, #e2e8f0)");

        // --- TOP BADGE & DATE ---
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.addClassNames("mb-s");

        Span statusBadge = new Span();
        if (isReady) {
            statusBadge.setText("SẴN SÀNG NHẬN SÁCH");
            statusBadge.addClassNames("bg-success-10", "text-success", "border", "border-success-30", "font-bold", "px-s", "py-xs", "rounded-full");
            statusBadge.getStyle().set("font-size", "11px");
        } else {
            statusBadge.setText("ĐANG XẾP HÀNG CHỜ (THỨ TỰ: #" + dto.queuePosition() + ")");
            statusBadge.addClassNames("bg-warning-10", "text-warning", "border", "border-warning-30", "font-bold", "px-s", "py-xs", "rounded-full");
            statusBadge.getStyle().set("font-size", "11px");
        }

        Span reservedDate = new Span(res.getReservedAt() != null ? "Đặt ngày: " + res.getReservedAt().format(DATETIME_FMT) : "");
        reservedDate.addClassNames("text-secondary");
        reservedDate.getStyle().set("font-size", "11px");

        topBar.add(statusBadge, reservedDate);

        // --- MIDDLE: COVER + BOOK INFO ---
        HorizontalLayout bookSection = new HorizontalLayout();
        bookSection.setWidthFull();
        bookSection.setSpacing(true);
        bookSection.setAlignItems(FlexComponent.Alignment.START);

        // Bìa sách
        Image coverImg = new Image(book != null && book.getCoverImageUrl() != null && !book.getCoverImageUrl().isBlank()
                ? book.getCoverImageUrl() : DEFAULT_COVER, "Bìa sách");
        coverImg.setWidth("68px");
        coverImg.setHeight("96px");
        coverImg.getStyle().set("object-fit", "cover");
        coverImg.getStyle().set("border-radius", "6px");
        coverImg.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");
        coverImg.getStyle().set("flex-shrink", "0");

        // Chi tiết
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.setWidthFull();

        Span title = new Span(book != null ? book.getTitle() : "Chưa xác định");
        title.addClassNames("font-bold", "text-primary", "mb-xs");
        title.getStyle().set("font-size", "14.5px");
        title.getStyle().set("line-height", "1.3");

        String authorsStr = (book != null && book.getAuthors() != null && !book.getAuthors().isEmpty())
                ? book.getAuthors().stream().map(Author::getFullName).collect(Collectors.joining(", "))
                : "Đang cập nhật";
        Span authorSpan = new Span("Tác giả: " + authorsStr);
        authorSpan.addClassNames("text-secondary", "mb-xs");
        authorSpan.getStyle().set("font-size", "12px");

        String categoriesStr = (book != null && book.getCategories() != null && !book.getCategories().isEmpty())
                ? book.getCategories().stream().map(Category::getName).collect(Collectors.joining(", "))
                : "Chung";
        Span catSpan = new Span("Thể loại: " + categoriesStr);
        catSpan.addClassNames("text-secondary", "mb-xs");
        catSpan.getStyle().set("font-size", "12px");

        info.add(title, authorSpan, catSpan);
        bookSection.add(coverImg, info);

        // --- HIGHLIGHT BOX (TÙY THEO READY HAY PENDING) ---
        Div highlightBox = new Div();
        highlightBox.setWidthFull();
        highlightBox.addClassNames("rounded-m", "p-s", "my-s", "border");

        if (isReady) {
            highlightBox.addClassNames("bg-success-5", "border-success-20");

            VerticalLayout hlLayout = new VerticalLayout();
            hlLayout.setPadding(false);
            hlLayout.setSpacing(false);

            String barcode = copy != null ? copy.getBarcode() : "Chưa gắn mã";
            String locName = (copy != null && copy.getLocation() != null)
                    ? copy.getLocation().getName() : "Quầy lưu thông tầng 1";

            Span copyInfo = new Span("Mã bản sao: " + barcode);
            copyInfo.addClassNames("text-primary", "font-medium", "mb-xs");
            copyInfo.getStyle().set("font-size", "12px");

            Span locInfo = new Span("Vị trí: " + locName);
            locInfo.addClassNames("text-secondary", "font-medium", "mb-xs");
            locInfo.getStyle().set("font-size", "12px");

            OffsetDateTime holdExpiresAt = res.getHoldExpiresAt();
            String expireStr = holdExpiresAt != null ? holdExpiresAt.format(DATETIME_FMT) : "Chưa xác định";

            Span expireInfo = new Span("Hạn chót đến nhận: " + expireStr);
            expireInfo.addClassNames("text-success", "font-bold");
            expireInfo.getStyle().set("font-size", "12.5px");

            hlLayout.add(copyInfo, locInfo, expireInfo);

            if (holdExpiresAt != null && OffsetDateTime.now().isAfter(holdExpiresAt.minusHours(24))) {
                Span urgentWarn = new Span("Sắp hết thời hạn giữ sách! Vui lòng đến thư viện nhận trước hạn.");
                urgentWarn.addClassNames("text-error", "font-bold", "mt-xs");
                urgentWarn.getStyle().set("font-size", "11px");
                hlLayout.add(urgentWarn);
            }

            highlightBox.add(hlLayout);
        } else {
            highlightBox.addClassNames("bg-contrast-5", "border-contrast-10");

            VerticalLayout hlLayout = new VerticalLayout();
            hlLayout.setPadding(false);
            hlLayout.setSpacing(false);

            Span queueInfo = new Span("Thứ tự chờ của bạn: #" + dto.queuePosition() + " trong hàng đợi.");
            queueInfo.addClassNames("text-primary", "font-semibold", "mb-xs");
            queueInfo.getStyle().set("font-size", "12px");

            Span noteInfo = new Span("Hệ thống sẽ tự động gán bản sao và giữ chỗ cho bạn ngay khi độc giả trước trả sách.");
            noteInfo.addClassNames("text-secondary");
            noteInfo.getStyle().set("font-size", "11.5px");

            hlLayout.add(queueInfo, noteInfo);
            highlightBox.add(hlLayout);
        }

        // --- FOOTER ACTION: CANCEL RESERVATION ---
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);

        Button cancelBtn = new Button("Hủy đặt trước", e -> confirmCancelReservation(res));
        cancelBtn.setIcon(VaadinIcon.CLOSE_SMALL.create());
        cancelBtn.setThemeName("error tertiary small");

        footer.add(cancelBtn);

        card.add(topBar, bookSection, highlightBox, footer);
        return card;
    }

    private void confirmCancelReservation(Reservation res) {
        String bookTitle = res.getBook() != null ? res.getBook().getTitle() : "cuốn sách này";

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Xác nhận hủy đặt trước");
        dialog.setText("Bạn có chắc chắn muốn hủy yêu cầu đặt trước sách \"" + bookTitle + "\" không? Vị trí chờ hoặc bản sao giữ chỗ sẽ được chuyển nhượng cho bạn đọc tiếp theo.");
        dialog.setCancelable(true);
        dialog.setCancelText("Đóng");
        dialog.setConfirmText("Đồng ý hủy");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                readerLoanService.cancelReservation(res.getId());
                toastService.success("Đã hủy thành công yêu cầu đặt trước cuốn sách: " + bookTitle);
                reloadData();
            } catch (Exception ex) {
                toastService.error("Không thể hủy đặt trước: " + ex.getMessage());
            }
        });

        dialog.open();
    }

    private void renderHistoryReservations() {
        historyGridWrapper.removeAll();
        emptyHistoryState.removeAll();

        List<Reservation> historyList = readerLoanService.getReservationHistory(currentMember.getId());

        if (historyList.isEmpty()) {
            emptyHistoryState.setVisible(true);
            historyGridWrapper.setVisible(false);

            Div emptyBox = new Div();
            emptyBox.setWidthFull();
            emptyBox.addClassNames("bg-base", "rounded-l", "border", "border-contrast-10", "p-xl", "text-center", "shadow-s");

            H3 title = new H3("Chưa có lịch sử đặt trước");
            title.addClassNames("font-bold", "text-primary", "mb-xs");
            title.getStyle().set("font-size", "14px");

            Paragraph desc = new Paragraph("Những lượt đặt trước sách sau khi đã nhận mượn, đã hủy hoặc hết hạn sẽ được lưu trữ tại đây.");
            desc.addClassNames("text-secondary", "max-w-md", "mx-auto");
            desc.getStyle().set("font-size", "12px");

            emptyBox.add(title, desc);
            emptyHistoryState.add(emptyBox);
            return;
        }

        emptyHistoryState.setVisible(false);
        historyGridWrapper.setVisible(true);

        Grid<Reservation> grid = new Grid<>();
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addClassNames("border-none");

        // Cột 1: Thông tin sách
        grid.addColumn(new ComponentRenderer<>(res -> {
            HorizontalLayout cell = new HorizontalLayout();
            cell.setAlignItems(FlexComponent.Alignment.CENTER);
            cell.setSpacing(true);

            Book b = res.getBook();
            Image img = new Image(b != null && b.getCoverImageUrl() != null && !b.getCoverImageUrl().isBlank()
                    ? b.getCoverImageUrl() : DEFAULT_COVER, "");
            img.setWidth("36px");
            img.setHeight("48px");
            img.getStyle().set("object-fit", "cover");
            img.getStyle().set("border-radius", "4px");
            img.getStyle().set("flex-shrink", "0");

            VerticalLayout textCol = new VerticalLayout();
            textCol.setPadding(false);
            textCol.setSpacing(false);

            Span titleSpan = new Span(b != null ? b.getTitle() : "Chưa xác định");
            titleSpan.addClassNames("font-semibold", "text-primary");
            titleSpan.getStyle().set("font-size", "13px");

            String authors = (b != null && b.getAuthors() != null)
                    ? b.getAuthors().stream().map(Author::getFullName).collect(Collectors.joining(", ")) : "";
            Span authorSpan = new Span(authors);
            authorSpan.addClassNames("text-secondary");
            authorSpan.getStyle().set("font-size", "11.5px");

            textCol.add(titleSpan, authorSpan);
            cell.add(img, textCol);
            return cell;
        })).setHeader("Đầu Sách").setAutoWidth(true).setFlexGrow(2);

        // Cột 2: Ngày đặt
        grid.addColumn(res -> res.getReservedAt() != null ? res.getReservedAt().format(DATETIME_FMT) : "-")
                .setHeader("Ngày Đặt Trước")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Cột 3: Trạng thái kết thúc
        grid.addColumn(new ComponentRenderer<>(res -> {
            Span badge = new Span();
            String st = res.getStatus();
            badge.getStyle().set("font-size", "11px");
            badge.addClassNames("px-s", "py-xs", "rounded-full", "font-semibold");

            if (ReservationStatus.FULFILLED.getId().equalsIgnoreCase(st)) {
                badge.setText("Đã mượn sách");
                badge.addClassNames("bg-success-10", "text-success");
            } else if (ReservationStatus.CANCELLED.getId().equalsIgnoreCase(st)) {
                badge.setText("Đã hủy");
                badge.addClassNames("bg-contrast-10", "text-secondary");
            } else if (ReservationStatus.EXPIRED.getId().equalsIgnoreCase(st)) {
                badge.setText("Hết hạn giữ chỗ");
                badge.addClassNames("bg-error-10", "text-error");
            } else {
                badge.setText(st);
                badge.addClassNames("bg-contrast-10", "text-secondary");
            }
            return badge;
        })).setHeader("Kết Quả").setAutoWidth(true).setFlexGrow(1);

        // Cột 4: Lý do / Ghi chú
        grid.addColumn(res -> {
            if (res.getCancelReason() != null && !res.getCancelReason().isBlank()) {
                return res.getCancelReason();
            }
            if (ReservationStatus.FULFILLED.getId().equalsIgnoreCase(res.getStatus())) {
                return "Đã nhận sách tại quầy";
            }
            if (ReservationStatus.EXPIRED.getId().equalsIgnoreCase(res.getStatus())) {
                return "Quá thời hạn giữ sách tại quầy";
            }
            return "-";
        }).setHeader("Ghi Chú / Diễn Giải").setAutoWidth(true).setFlexGrow(2);

        // Cột 5: Thời gian cập nhật
        grid.addColumn(res -> res.getUpdatedAt() != null ? res.getUpdatedAt().format(DATETIME_FMT) : "-")
                .setHeader("Thời Gian Hoàn Tất")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.setItems(historyList);
        historyGridWrapper.add(grid);
    }
}
