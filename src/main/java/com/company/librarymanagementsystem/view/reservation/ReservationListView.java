package com.company.librarymanagementsystem.view.reservation;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.security.AccessControlService;
import com.company.librarymanagementsystem.security.Permissions;
import com.company.librarymanagementsystem.service.MemberService;
import com.company.librarymanagementsystem.service.ReservationService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.loan.LoanCreateView;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.router.QueryParameters;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "reservations", layout = MainView.class)
@ViewController("Reservation.list")
@ViewDescriptor("reservation-list-view.xml")
@LookupComponent("reservationsDataGrid")
@DialogMode(width = "85em")
public class ReservationListView extends StandardView {

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @ViewComponent
    private Div kpiContainer;

    @ViewComponent
    private TextField searchField;
    @ViewComponent
    private Select<String> statusFilterSelect;
    @ViewComponent
    private CollectionLoader<Reservation> reservationsDl;
    @ViewComponent
    private DataGrid<Reservation> reservationsDataGrid;

    @ViewComponent
    private Button createReservationBtn;
    @ViewComponent
    private Button assignCopyBtn;
    @ViewComponent
    private Button loanBookBtn;
    @ViewComponent
    private Button cancelReservationBtn;
    @ViewComponent
    private Button processExpiredBtn;
    @ViewComponent
    private Button refreshBtn;

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Subscribe
    public void onInit(InitEvent event) {
        initFilters();
        initGridRenderers();
        initActionButtons();
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        // Áp dụng phân quyền chi tiết
        accessControlService.applyPermission(createReservationBtn, Permissions.RESERVATION_MANAGE);
        accessControlService.applyPermission(assignCopyBtn, Permissions.RESERVATION_MANAGE);
        accessControlService.applyPermission(cancelReservationBtn, Permissions.RESERVATION_CANCEL);
        accessControlService.applyPermission(loanBookBtn, Permissions.LOAN_CREATE);
        accessControlService.applyPermission(processExpiredBtn, Permissions.RESERVATION_MANAGE);

        loadData();
    }

    private void initFilters() {
        statusFilterSelect.setItems(Arrays.asList("", "READY", "PENDING", "FULFILLED", "CANCELLED", "EXPIRED"));
        statusFilterSelect.setItemLabelGenerator(s -> switch (s) {
            case "READY" -> "Sách đã về - Chờ nhận";
            case "PENDING" -> "Đang chờ sách";
            case "FULFILLED" -> "Đã nhận sách";
            case "CANCELLED" -> "Đã hủy";
            case "EXPIRED" -> "Hết hạn nhận";
            default -> "Tất cả trạng thái";
        });
        statusFilterSelect.setValue("");
    }

    private void initGridRenderers() {
        // Căn giữa thời điểm đặt
        reservationsDataGrid.getColumnByKey("reservedAtCol").setRenderer(new ComponentRenderer<>(res -> {
            if (res.getReservedAt() != null) {
                return createCenteredCell(new Span(res.getReservedAt().format(dtf)));
            }
            return createCenteredCell(new Span("-"));
        }));

        // Bản sao giữ chỗ
        reservationsDataGrid.getColumnByKey("bookCopyCol").setRenderer(new ComponentRenderer<>(res -> {
            BookCopy copy = res.getBookCopy();
            if (copy != null) {
                String loc = copy.getLocation() != null ? " (" + copy.getLocation().getName() + ")" : "";
                Span badge = new Span(copy.getBarcode() + loc);
                badge.getStyle()
                        .set("background", "#eff6ff")
                        .set("color", "#1d4ed8")
                        .set("padding", "3px 8px")
                        .set("border-radius", "4px")
                        .set("font-weight", "600")
                        .set("font-size", "12px");
                return createCenteredCell(badge);
            }
            return createCenteredCell(new Span("-"));
        }));

        // Hạn nhận sách
        reservationsDataGrid.getColumnByKey("holdExpiresAtCol").setRenderer(new ComponentRenderer<>(res -> {
            if (res.getHoldExpiresAt() != null) {
                boolean isExpired = OffsetDateTime.now().isAfter(res.getHoldExpiresAt());
                boolean isReady = ReservationStatus.READY.getId().equalsIgnoreCase(res.getStatus());

                Span span = new Span(res.getHoldExpiresAt().format(dtf));
                span.getStyle()
                        .set("padding", "3px 8px")
                        .set("border-radius", "4px")
                        .set("font-size", "12px")
                        .set("font-weight", "600");

                if (isReady) {
                    if (isExpired) {
                        span.setText(res.getHoldExpiresAt().format(dtf) + " (Quá hạn)");
                        span.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
                    } else {
                        span.getStyle().set("background", "#ecfdf5").set("color", "#047857");
                    }
                } else {
                    span.getStyle().set("color", "#64748b");
                }
                return createCenteredCell(span);
            }
            return createCenteredCell(new Span("-"));
        }));

        // Trạng thái mượn đặt trước
        reservationsDataGrid.getColumnByKey("statusCol").setRenderer(new ComponentRenderer<>(res -> {
            String st = res.getStatus();
            Span badge = new Span();
            badge.getStyle()
                    .set("padding", "4px 10px")
                    .set("border-radius", "6px")
                    .set("font-size", "12px")
                    .set("font-weight", "600");

            if (ReservationStatus.READY.getId().equalsIgnoreCase(st)) {
                badge.setText("SẴN SÀNG NHẬN");
                badge.getStyle().set("background", "#dcfce7").set("color", "#15803d").set("border",
                        "1px solid #86efac");
            } else if (ReservationStatus.PENDING.getId().equalsIgnoreCase(st)) {
                badge.setText("ĐANG CHỜ");
                badge.getStyle().set("background", "#fef3c7").set("color", "#b45309");
            } else if (ReservationStatus.FULFILLED.getId().equalsIgnoreCase(st)) {
                badge.setText("ĐÃ NHẬN");
                badge.getStyle().set("background", "#e0e7ff").set("color", "#3730a3");
            } else if (ReservationStatus.CANCELLED.getId().equalsIgnoreCase(st)) {
                badge.setText("ĐÃ HỦY");
                badge.getStyle().set("background", "#f1f5f9").set("color", "#64748b");
            } else if (ReservationStatus.EXPIRED.getId().equalsIgnoreCase(st)) {
                badge.setText("HẾT HẠN");
                badge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
            } else {
                badge.setText(st);
            }
            return createCenteredCell(badge);
        }));

        // Thao tác trực tiếp trên từng dòng
        reservationsDataGrid.getColumnByKey("actionCol").setRenderer(new ComponentRenderer<>(res -> {
            HorizontalLayout cell = new HorizontalLayout();
            cell.setWidthFull();
            cell.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            cell.setAlignItems(FlexComponent.Alignment.CENTER);
            cell.setSpacing(true);

            String st = res.getStatus();
            if (ReservationStatus.READY.getId().equalsIgnoreCase(st)) {
                Button loanBtn = new Button("Chuyển lập phiếu", VaadinIcon.FILE_TEXT.create(), e -> navigateToLoanCreate(res));
                loanBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                loanBtn.getStyle().set("font-weight", "600");
                cell.add(loanBtn);
            } else if (ReservationStatus.PENDING.getId().equalsIgnoreCase(st)) {
                Button assignBtn = new Button("Gán bản sao", VaadinIcon.BOOK.create(), e -> assignCopyForReservation(res));
                assignBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
                cell.add(assignBtn);
            } else {
                Span doneSpan = new Span("-");
                doneSpan.getStyle().set("color", "#94a3b8");
                cell.add(doneSpan);
            }
            return cell;
        }));
    }

    private void initActionButtons() {
        createReservationBtn.addClickListener(this::onCreateReservationClick);
        assignCopyBtn.addClickListener(this::onAssignCopyClick);
        cancelReservationBtn.addClickListener(this::onCancelReservationClick);
        loanBookBtn.addClickListener(this::onLoanBookClick);
        processExpiredBtn.addClickListener(this::onProcessExpiredClick);
        refreshBtn.addClickListener(e -> loadData());
    }

    private void loadData() {
        // Tự động quét điều phối bản sao AVAILABLE mới cho các lượt đặt trước ĐANG CHỜ
        try {
            int autoMatched = reservationService.autoMatchPendingReservations();
            if (autoMatched > 0) {
                toastService
                        .info(String.format("Đã tự động điều phối sách sẵn sàng cho %d lượt đặt trước!", autoMatched));
            }
        } catch (Exception ignored) {
        }

        String status = statusFilterSelect.getValue();
        String search = searchField.getValue();

        reservationsDl.setParameter("statusFilter", (status == null || status.isBlank()) ? null : status);
        reservationsDl.setParameter("searchText", (search == null || search.isBlank()) ? null : search.trim());
        reservationsDl.load();

        updateStats();
    }

    @Subscribe("searchBtn")
    public void onSearchBtnClick(ClickEvent<Button> event) {
        reservationsDl.setFirstResult(0);
        loadData();
    }

    @Subscribe("clearFilterBtn")
    public void onClearFilterBtnClick(ClickEvent<Button> event) {
        searchField.clear();
        statusFilterSelect.setValue("");
        reservationsDl.setFirstResult(0);
        loadData();
    }

    private void updateStats() {
        if (kpiContainer == null) return;
        kpiContainer.removeAll();

        ReservationService.ReservationStats stats = reservationService.getStats();

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        grid.add(createKpiCard(
                "Đang Chờ Sách",
                String.valueOf(stats.totalPending()),
                "Độc giả đang xếp hàng chờ",
                stats.totalPending() > 0 ? "bg-warning-10 text-warning border-warning-20" : "bg-base border-contrast-10",
                stats.totalPending() > 0 ? "text-warning font-bold" : "text-primary"
        ));

        grid.add(createKpiCard(
                "Sách Đã Về Chờ Nhận",
                String.valueOf(stats.totalReady()),
                "Đang giữ tại quầy lưu thông",
                stats.totalReady() > 0 ? "bg-success-10 text-success border-success-30" : "bg-base border-contrast-10",
                stats.totalReady() > 0 ? "text-success font-bold" : "text-primary"
        ));

        grid.add(createKpiCard(
                "Đã Nhận Thành Công",
                String.valueOf(stats.totalFulfilled()),
                "Đã chuyển thành phiếu mượn",
                "bg-base border-contrast-10",
                "text-primary"
        ));

        grid.add(createKpiCard(
                "Hết Hạn / Đã Hủy",
                String.valueOf(stats.totalExpiredOrCancelled()),
                "Quá hạn nhận hoặc hủy yêu cầu",
                stats.totalExpiredOrCancelled() > 0 ? "bg-error-10 text-error border-error-50" : "bg-base border-contrast-10",
                stats.totalExpiredOrCancelled() > 0 ? "text-error font-bold" : "text-primary"
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
     * Dialog Tạo yêu cầu đặt trước tại quầy (L07)
     */
    private void onCreateReservationClick(ClickEvent<Button> event) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Đặt Trước Sách Tại Quầy");
        dialog.setWidth("38em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        ComboBox<Member> memberCombo = new ComboBox<>("Chọn độc giả");
        memberCombo.setWidthFull();
        memberCombo.setPlaceholder("Tìm theo mã thẻ, họ tên, CCCD...");
        memberCombo.setRequired(true);

        List<Member> members = dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.status = 'ACTIVE' order by m.fullName")
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("memberType", FetchPlan.BASE))
                .list();
        memberCombo.setItems(members);
        memberCombo.setItemLabelGenerator(m -> String.format("[%s] %s (%s)",
                m.getMemberCode(), m.getFullName(), m.getMemberType() != null ? m.getMemberType().getName() : ""));

        Div eligibilityNote = new Div();
        eligibilityNote.getStyle().set("font-size", "12px").set("padding", "6px 10px").set("border-radius", "6px")
                .set("display", "none");

        memberCombo.addValueChangeListener(e -> {
            Member m = e.getValue();
            if (m != null) {
                MemberService.EligibilityResult el = memberService.checkEligibility(m);
                eligibilityNote.getStyle().set("display", "block");
                if (el.isEligible()) {
                    eligibilityNote.setText(String.format("Độc giả đủ điều kiện (Đang mượn: %d, Nợ phạt: %,.0f VNĐ)",
                            el.getCurrentLoans(), el.getUnpaidFines()));
                    eligibilityNote.getStyle().set("background", "#ecfdf5").set("color", "#047857");
                } else {
                    eligibilityNote.setText("Chặn: " + String.join("; ", el.getBlockingReasons()));
                    eligibilityNote.getStyle().set("background", "#fef2f2").set("color", "#b91c1c");
                }
            } else {
                eligibilityNote.getStyle().set("display", "none");
            }
        });

        ComboBox<Book> bookCombo = new ComboBox<>("Chọn đầu sách cần đặt trước");
        bookCombo.setWidthFull();
        bookCombo.setPlaceholder("Tìm theo tên sách, ISBN...");
        bookCombo.setRequired(true);

        List<Book> books = dataManager.load(Book.class)
                .query("select b from Book b order by b.title")
                .fetchPlan(FetchPlan.BASE)
                .list();
        bookCombo.setItems(books);
        bookCombo.setItemLabelGenerator(
                b -> String.format("%s (ISBN: %s)", b.getTitle(), b.getIsbn() != null ? b.getIsbn() : "-"));

        Div bookAvailabilityNote = new Div();
        bookAvailabilityNote.getStyle().set("font-size", "12px").set("padding", "6px 10px").set("border-radius", "6px")
                .set("display", "none");

        bookCombo.addValueChangeListener(e -> {
            Book b = e.getValue();
            if (b != null) {
                Long avail = dataManager.loadValue(
                        "select count(bc) from BookCopy bc where bc.book.id = :bookId and bc.status = 'AVAILABLE'",
                        Long.class)
                        .parameter("bookId", b.getId())
                        .one();
                Long total = dataManager.loadValue(
                        "select count(bc) from BookCopy bc where bc.book.id = :bookId", Long.class)
                        .parameter("bookId", b.getId())
                        .one();

                bookAvailabilityNote.getStyle().set("display", "block");
                if (avail > 0) {
                    bookAvailabilityNote.setText(String.format(
                            "Lưu ý: Đầu sách này hiện còn %d / %d bản sao sẵn sàng (AVAILABLE) trên kệ!", avail,
                            total));
                    bookAvailabilityNote.getStyle().set("background", "#fffbeb").set("color", "#b45309");
                } else {
                    bookAvailabilityNote.setText(String.format(
                            "Hợp lệ để đặt trước: Toàn bộ %d bản sao đều đang được mượn hoặc giữ chỗ.", total));
                    bookAvailabilityNote.getStyle().set("background", "#ecfdf5").set("color", "#047857");
                }
            } else {
                bookAvailabilityNote.getStyle().set("display", "none");
            }
        });

        layout.add(memberCombo, eligibilityNote, bookCombo, bookAvailabilityNote);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận Đặt trước", e -> {
            Member m = memberCombo.getValue();
            Book b = bookCombo.getValue();
            if (m == null || b == null) {
                toastService.warning("Vui lòng chọn cả độc giả và đầu sách!");
                return;
            }

            try {
                Reservation res = reservationService.createReservation(m, b);
                toastService.success(String.format("Đặt trước thành công sách '%s' cho độc giả %s!",
                        b.getTitle(), m.getFullName()));
                dialog.close();
                loadData();
            } catch (Exception ex) {
                toastService.error(ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Đóng", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    /**
     * Hủy yêu cầu đặt trước sách
     */
    private void onCancelReservationClick(ClickEvent<Button> event) {
        Reservation selected = reservationsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 lượt đặt trước trên bảng để hủy!");
            return;
        }

        if (ReservationStatus.FULFILLED.getId().equalsIgnoreCase(selected.getStatus())
                || ReservationStatus.CANCELLED.getId().equalsIgnoreCase(selected.getStatus())
                || ReservationStatus.EXPIRED.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Chỉ có thể hủy lượt đặt trước đang ở trạng thái 'ĐANG CHỜ' hoặc 'SẴN SÀNG NHẬN'!");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Xác nhận Hủy Đặt Trước");
        dialog.setWidth("32em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        String bookTitle = selected.getBook() != null ? selected.getBook().getTitle() : "";
        String memberName = selected.getMember() != null ? selected.getMember().getFullName() : "";

        Span info = new Span(String.format("Bạn có chắc chắn muốn hủy lượt đặt trước cuốn '%s' của độc giả %s?",
                bookTitle, memberName));
        info.getStyle().set("font-size", "14px");

        TextField reasonField = new TextField("Lý do hủy");
        reasonField.setWidthFull();
        reasonField.setPlaceholder("Nhập lý do hủy nếu có (ví dụ: Độc giả không còn nhu cầu...)");

        layout.add(info, reasonField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận Hủy", e -> {
            try {
                reservationService.cancelReservation(selected.getId(), reasonField.getValue());
                toastService.success("Đã hủy lượt đặt trước thành công!");
                dialog.close();
                loadData();
            } catch (Exception ex) {
                toastService.error("Lỗi khi hủy: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Không hủy", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    /**
     * Gán thủ công một bản sao đang có sẵn (AVAILABLE) cho lượt đặt trước đang chờ
     * (PENDING)
     */
    private void onAssignCopyClick(ClickEvent<Button> event) {
        Reservation selected = reservationsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 lượt đặt trước trên bảng!");
            return;
        }

        if (!ReservationStatus.PENDING.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Chỉ có thể gán bản sao cho lượt đặt trước đang ở trạng thái 'ĐANG CHỜ'!");
            return;
        }
        assignCopyForReservation(selected);
    }

    private void assignCopyForReservation(Reservation res) {
        if (res == null) return;
        try {
            BookCopy copy = reservationService.assignAvailableCopy(res.getId());
            String memberName = res.getMember() != null ? res.getMember().getFullName() : "";
            toastService.success(String.format("Đã gán bản sao '%s' cho độc giả '%s'! Trạng thái chuyển sang READY.",
                    copy.getBarcode(), memberName));
            loadData();
        } catch (Exception ex) {
            toastService.error(ex.getMessage());
        }
    }

    /**
     * Chuyển sang lập phiếu mượn cho lượt đặt trước có sách sẵn sàng
     */
    private void onLoanBookClick(ClickEvent<Button> event) {
        Reservation selected = reservationsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 lượt đặt trước có sách sẵn sàng trên bảng!");
            return;
        }
        navigateToLoanCreate(selected);
    }

    private void navigateToLoanCreate(Reservation res) {
        if (res == null) {
            return;
        }

        if (!ReservationStatus.READY.getId().equalsIgnoreCase(res.getStatus())) {
            toastService.warning("Chỉ có thể chuyển lập phiếu mượn cho lượt đặt trước đã có sách sẵn sàng (SÁCH ĐÃ VỀ - CHỜ NHẬN)!");
            return;
        }

        Map<String, List<String>> params = new HashMap<>();
        if (res.getMember() != null) {
            params.put("memberId", List.of(res.getMember().getId().toString()));
        }
        if (res.getBookCopy() != null) {
            params.put("barcode", List.of(res.getBookCopy().getBarcode()));
        }

        viewNavigators.view(this, LoanCreateView.class)
                .withQueryParameters(new QueryParameters(params))
                .navigate();
    }

    /**
     * Quét và xử lý các lượt đặt trước quá hạn giữ chỗ (BR12)
     */
    private void onProcessExpiredClick(ClickEvent<Button> event) {
        try {
            int count = reservationService.processExpiredReservations();
            if (count > 0) {
                toastService.success(String.format("Đã xử lý %d lượt đặt trước quá hạn nhận sách!", count));
            } else {
                toastService.info("Không có lượt đặt trước nào bị quá hạn giữ chỗ.");
            }
            loadData();
        } catch (Exception ex) {
            toastService.error("Lỗi xử lý hết hạn: " + ex.getMessage());
        }
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
