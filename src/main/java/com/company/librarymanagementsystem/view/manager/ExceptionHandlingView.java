package com.company.librarymanagementsystem.view.manager;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.AuditLogService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "exception-handling", layout = MainView.class)
@ViewController("ExceptionHandlingView")
@ViewDescriptor("exception-handling-view.xml")
public class ExceptionHandlingView extends StandardView {

    @ViewComponent
    private JmixTabSheet exceptionTabSheet;
    @ViewComponent
    private Div unlockTabContent;
    @ViewComponent
    private Div cancelLoanTabContent;
    @ViewComponent
    private Div restoreBookTabContent;
    @ViewComponent
    private Button refreshBtn;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private ToastService toastService;
    @Autowired
    private AuditLogService auditLogService;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String currentSearchKeyword = "";

    @Subscribe
    public void onInit(final InitEvent event) {
        initTabs();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        refreshBtn.addClickListener(e -> refreshCurrentTab());
        // Khởi tạo nội dung cho cả 3 tab
        renderUnlockMembersTab();
        renderCancelLoansTab();
        renderRestoreLostBooksTab();
    }

    private void initTabs() {
        exceptionTabSheet.addSelectedChangeListener(event -> {
            Tab selected = event.getSelectedTab();
            if (selected == null || selected.getId().isEmpty()) return;

            currentSearchKeyword = "";
            String tabId = selected.getId().get();
            switch (tabId) {
                case "unlockMemberTab" -> renderUnlockMembersTab();
                case "cancelLoanTab" -> renderCancelLoansTab();
                case "restoreLostBookTab" -> renderRestoreLostBooksTab();
            }
        });
    }

    private void refreshCurrentTab() {
        Tab selected = exceptionTabSheet.getSelectedTab();
        if (selected == null || selected.getId().isEmpty()) return;
        String tabId = selected.getId().get();
        switch (tabId) {
            case "unlockMemberTab" -> renderUnlockMembersTab();
            case "cancelLoanTab" -> renderCancelLoansTab();
            case "restoreLostBookTab" -> renderRestoreLostBooksTab();
        }
        toastService.success("Đã làm mới dữ liệu xử lý ngoại lệ!");
    }

    // ==========================================
    // TAB 1: MỞ KHÓA ĐỘC GIẢ (BLOCKED / SUSPENDED)
    // ==========================================
    private void renderUnlockMembersTab() {
        if (unlockTabContent == null) return;
        unlockTabContent.removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();

        // Search bar & Description
        HorizontalLayout filterRow = new HorizontalLayout();
        filterRow.setWidthFull();
        filterRow.setAlignItems(FlexComponent.Alignment.CENTER);

        TextField searchField = new TextField();
        searchField.setPlaceholder("Tìm theo mã thẻ, họ tên, SĐT độc giả...");
        searchField.setWidth("380px");
        searchField.setValue(currentSearchKeyword);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);

        Span infoSpan = new Span("Chỉ hiển thị độc giả có trạng thái Bị khóa (BLOCKED) hoặc Tạm khóa (SUSPENDED)");
        infoSpan.addClassNames("text-secondary", "text-s");

        filterRow.add(searchField, infoSpan);
        layout.add(filterRow);

        // Grid
        Grid<Member> grid = new Grid<>();
        grid.setWidthFull();
        grid.setMinHeight("420px");
        grid.addClassNames("rounded-m", "border", "border-contrast-10");

        grid.addColumn(Member::getMemberCode).setHeader("Mã thẻ").setAutoWidth(true);
        grid.addColumn(Member::getFullName).setHeader("Họ và tên").setAutoWidth(true);
        grid.addColumn(m -> (m.getUser() != null && m.getUser().getPhone() != null && !m.getUser().getPhone().isBlank()) 
                ? m.getUser().getPhone() 
                : (m.getIdNumber() != null ? m.getIdNumber() : "-")).setHeader("SĐT / CCCD").setAutoWidth(true);
        grid.addColumn(m -> m.getMemberType() != null ? m.getMemberType().getName() : "-").setHeader("Loại độc giả").setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(m -> {
            Span badge = new Span();
            if ("BLOCKED".equalsIgnoreCase(m.getStatus())) {
                badge.setText("Bị khóa (BLOCKED)");
                badge.addClassNames("badge", "error");
            } else if ("SUSPENDED".equalsIgnoreCase(m.getStatus())) {
                badge.setText("Tạm khóa (SUSPENDED)");
                badge.addClassNames("badge", "warning");
            } else {
                badge.setText(m.getStatus());
                badge.addClassNames("badge");
            }
            return badge;
        })).setHeader("Trạng thái").setAutoWidth(true);

        grid.addColumn(m -> m.getStatusReason() != null ? m.getStatusReason() : "-").setHeader("Lý do khóa").setAutoWidth(true);

        grid.addComponentColumn(member -> {
            Button unlockBtn = new Button("Mở khóa thẻ", VaadinIcon.UNLOCK.create());
            unlockBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            unlockBtn.addClickListener(e -> showUnlockMemberDialog(member));
            return unlockBtn;
        }).setHeader("Thao tác").setAutoWidth(true);

        Runnable dataLoader = () -> {
            String kw = searchField.getValue() != null ? searchField.getValue().trim() : "";
            List<Member> members = dataManager.load(Member.class)
                    .query("select m from LibraryMember m where m.deletedAt is null " +
                            "and m.status in ('BLOCKED', 'SUSPENDED') " +
                            "and (:kw = '' or lower(m.memberCode) like lower(concat('%', :kw, '%')) " +
                            "or lower(m.fullName) like lower(concat('%', :kw, '%')) " +
                            "or lower(m.idNumber) like lower(concat('%', :kw, '%')) " +
                            "or (m.user is not null and lower(m.user.phone) like lower(concat('%', :kw, '%')))) " +
                            "order by m.updatedAt desc")
                    .parameter("kw", kw)
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                            .add("memberType", FetchPlan.BASE)
                            .add("user", FetchPlan.BASE))
                    .list();
            grid.setItems(members);
        };

        searchField.addValueChangeListener(e -> {
            currentSearchKeyword = e.getValue();
            dataLoader.run();
        });

        dataLoader.run();
        layout.add(grid);
        unlockTabContent.add(layout);
    }

    private void showUnlockMemberDialog(Member member) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Xác nhận mở khóa thẻ độc giả");
        dialog.setWidth("480px");

        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);

        Div memberCard = new Div();
        memberCard.addClassNames("p-s", "bg-contrast-5", "rounded-m", "w-full", "text-s");
        memberCard.add(new Span(String.format("Độc giả: %s (%s) - Trạng thái hiện tại: %s",
                member.getFullName(), member.getMemberCode(), member.getStatus())));
        form.add(memberCard);

        TextArea reasonField = new TextArea("Lý do mở khóa ngoại lệ (Lưu vết kiểm toán)");
        reasonField.setPlaceholder("Nhập căn cứ xử lý (VD: Độc giả đã bồi thường đủ, quyết định giảm chế tài của Ban Giám đốc...)");
        reasonField.setWidthFull();
        reasonField.setRequired(true);
        form.add(reasonField);

        Button confirmBtn = new Button("Xác nhận mở khóa", VaadinIcon.CHECK.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        confirmBtn.addClickListener(e -> {
            String reason = reasonField.getValue() != null ? reasonField.getValue().trim() : "";
            if (reason.isEmpty()) {
                toastService.error("Vui lòng nhập lý do mở khóa ngoại lệ!");
                return;
            }

            try {
                Member reloaded = dataManager.load(Member.class).id(member.getId()).one();
                String oldStatus = reloaded.getStatus();
                reloaded.setStatus(MemberStatus.ACTIVE.getId());
                reloaded.setStatusReason("Đã mở khóa bởi Manager: " + reason);
                dataManager.save(reloaded);

                auditLogService.log("UNBLOCK_MEMBER", "Member", member.getMemberCode(),
                        String.format("Mở khóa thẻ độc giả %s từ [%s] sang [ACTIVE]. Lý do: %s",
                                member.getFullName(), oldStatus, reason));

                toastService.success("Đã mở khóa thành công thẻ độc giả " + member.getMemberCode());
                dialog.close();
                renderUnlockMembersTab();
            } catch (Exception ex) {
                toastService.error("Lỗi khi mở khóa: " + ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Đóng", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    // ==========================================
    // TAB 2: HỦY PHIẾU MƯỢN LẬP SAI SÓT
    // ==========================================
    private void renderCancelLoansTab() {
        if (cancelLoanTabContent == null) return;
        cancelLoanTabContent.removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();

        HorizontalLayout filterRow = new HorizontalLayout();
        filterRow.setWidthFull();
        filterRow.setAlignItems(FlexComponent.Alignment.CENTER);

        TextField searchField = new TextField();
        searchField.setPlaceholder("Tìm theo mã phiếu, mã thẻ, mã vạch sách...");
        searchField.setWidth("380px");
        searchField.setValue(currentSearchKeyword);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);

        Span infoSpan = new Span("Chỉ hiển thị các lượt mượn đang diễn ra (BORROWED) để Manager xử lý hủy phiếu nhầm lẫn");
        infoSpan.addClassNames("text-secondary", "text-s");

        filterRow.add(searchField, infoSpan);
        layout.add(filterRow);

        Grid<LoanItem> grid = new Grid<>();
        grid.setWidthFull();
        grid.setMinHeight("420px");
        grid.addClassNames("rounded-m", "border", "border-contrast-10");

        grid.addColumn(li -> li.getLoan() != null ? li.getLoan().getLoanCode() : "-").setHeader("Mã phiếu").setAutoWidth(true);
        grid.addColumn(li -> {
            if (li.getLoan() != null && li.getLoan().getMember() != null) {
                return li.getLoan().getMember().getFullName() + " (" + li.getLoan().getMember().getMemberCode() + ")";
            }
            return "-";
        }).setHeader("Độc giả").setAutoWidth(true);

        grid.addColumn(li -> li.getBookCopy() != null ? li.getBookCopy().getBarcode() : "-").setHeader("Mã vạch").setAutoWidth(true);
        grid.addColumn(li -> {
            if (li.getBookCopy() != null && li.getBookCopy().getBook() != null) {
                return li.getBookCopy().getBook().getTitle();
            }
            return "-";
        }).setHeader("Nhan đề sách").setAutoWidth(true);

        grid.addColumn(li -> li.getDueDate() != null ? li.getDueDate().format(shortDateFormatter) : "-").setHeader("Hạn trả").setAutoWidth(true);

        grid.addComponentColumn(item -> {
            Button cancelBtn = new Button("Hủy lượt mượn", VaadinIcon.CLOSE_CIRCLE.create());
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            cancelBtn.addClickListener(e -> showCancelLoanDialog(item));
            return cancelBtn;
        }).setHeader("Thao tác").setAutoWidth(true);

        Runnable dataLoader = () -> {
            String kw = searchField.getValue() != null ? searchField.getValue().trim() : "";
            List<LoanItem> items = dataManager.load(LoanItem.class)
                    .query("select li from LoanItem li where li.status = 'BORROWED' " +
                            "and (:kw = '' or lower(li.loan.loanCode) like lower(concat('%', :kw, '%')) " +
                            "or lower(li.loan.member.memberCode) like lower(concat('%', :kw, '%')) " +
                            "or lower(li.bookCopy.barcode) like lower(concat('%', :kw, '%')) " +
                            "or lower(li.bookCopy.book.title) like lower(concat('%', :kw, '%'))) " +
                            "order by li.createdAt desc")
                    .parameter("kw", kw)
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                            .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE).add("member", FetchPlan.BASE))
                            .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE)))
                    .maxResults(50)
                    .list();
            grid.setItems(items);
        };

        searchField.addValueChangeListener(e -> {
            currentSearchKeyword = e.getValue();
            dataLoader.run();
        });

        dataLoader.run();
        layout.add(grid);
        cancelLoanTabContent.add(layout);
    }

    private void showCancelLoanDialog(LoanItem item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Xác nhận hủy lượt mượn do lập sai");
        dialog.setWidth("500px");

        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);

        String bookTitle = item.getBookCopy() != null && item.getBookCopy().getBook() != null
                ? item.getBookCopy().getBook().getTitle() : "";
        String barcode = item.getBookCopy() != null ? item.getBookCopy().getBarcode() : "";
        String memberName = item.getLoan() != null && item.getLoan().getMember() != null
                ? item.getLoan().getMember().getFullName() : "";

        Div infoBox = new Div();
        infoBox.addClassNames("p-s", "bg-error-10", "rounded-m", "w-full", "text-s", "text-error");
        infoBox.add(new Span(String.format("Hành động này sẽ chuyển sách '%s' (Mã %s) của độc giả %s về trạng thái Sẵn sàng (AVAILABLE).",
                bookTitle, barcode, memberName)));
        form.add(infoBox);

        TextArea reasonField = new TextArea("Lý do hủy ngoại lệ");
        reasonField.setPlaceholder("VD: Thủ thư chọn nhầm mã vạch, độc giả đổi ý ngay tại quầy...");
        reasonField.setWidthFull();
        reasonField.setRequired(true);
        form.add(reasonField);

        Button confirmBtn = new Button("Xác nhận hủy phiếu", VaadinIcon.TRASH.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirmBtn.addClickListener(e -> {
            String reason = reasonField.getValue() != null ? reasonField.getValue().trim() : "";
            if (reason.isEmpty()) {
                toastService.error("Vui lòng nhập lý do hủy ngoại lệ!");
                return;
            }

            try {
                LoanItem reloadedItem = dataManager.load(LoanItem.class).id(item.getId())
                        .fetchPlan(b -> b.addFetchPlan(FetchPlan.BASE).add("loan", FetchPlan.BASE).add("bookCopy", FetchPlan.BASE))
                        .one();

                reloadedItem.setStatus(LoanItemStatus.RETURNED.getId());
                reloadedItem.setNote("Đã hủy ngoại lệ bởi Manager: " + reason);

                BookCopy copy = reloadedItem.getBookCopy();
                if (copy != null) {
                    BookCopy reloadedCopy = dataManager.load(BookCopy.class).id(copy.getId()).one();
                    reloadedCopy.setStatus(BookCopyStatus.AVAILABLE.getId());
                    dataManager.save(reloadedCopy);
                }

                dataManager.save(reloadedItem);

                auditLogService.log("CANCEL_INCORRECT_LOAN", "LoanItem", String.valueOf(reloadedItem.getId()),
                        String.format("Hủy lượt mượn sách %s (Mã %s) thuộc phiếu %s. Lý do: %s",
                                bookTitle, barcode, item.getLoan() != null ? item.getLoan().getLoanCode() : "", reason));

                toastService.success("Đã hủy thành công lượt mượn sách " + barcode);
                dialog.close();
                renderCancelLoansTab();
            } catch (Exception ex) {
                toastService.error("Lỗi khi hủy lượt mượn: " + ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Đóng", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    // ==========================================
    // TAB 3: PHỤC HỒI SÁCH MẤT TÌM LẠI ĐƯỢC
    // ==========================================
    private void renderRestoreLostBooksTab() {
        if (restoreBookTabContent == null) return;
        restoreBookTabContent.removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();

        HorizontalLayout filterRow = new HorizontalLayout();
        filterRow.setWidthFull();
        filterRow.setAlignItems(FlexComponent.Alignment.CENTER);

        TextField searchField = new TextField();
        searchField.setPlaceholder("Tìm theo mã vạch, nhan đề sách đã báo mất...");
        searchField.setWidth("380px");
        searchField.setValue(currentSearchKeyword);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);

        Span infoSpan = new Span("Danh sách bản sao sách đang ở trạng thái Báo mất (LOST) hoặc Hư hỏng (DAMAGED)");
        infoSpan.addClassNames("text-secondary", "text-s");

        filterRow.add(searchField, infoSpan);
        layout.add(filterRow);

        Grid<BookCopy> grid = new Grid<>();
        grid.setWidthFull();
        grid.setMinHeight("420px");
        grid.addClassNames("rounded-m", "border", "border-contrast-10");

        grid.addColumn(BookCopy::getBarcode).setHeader("Mã vạch").setAutoWidth(true);
        grid.addColumn(bc -> bc.getBook() != null ? bc.getBook().getTitle() : "-").setHeader("Nhan đề sách").setAutoWidth(true);
        grid.addColumn(bc -> bc.getBook() != null && bc.getBook().getPublisher() != null ? bc.getBook().getPublisher().getName() : "-").setHeader("NXB").setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(bc -> {
            Span badge = new Span();
            if ("LOST".equalsIgnoreCase(bc.getStatus())) {
                badge.setText("Báo mất (LOST)");
                badge.addClassNames("badge", "error");
            } else if ("DAMAGED".equalsIgnoreCase(bc.getStatus())) {
                badge.setText("Hư hỏng (DAMAGED)");
                badge.addClassNames("badge", "warning");
            } else {
                badge.setText(bc.getStatus());
                badge.addClassNames("badge");
            }
            return badge;
        })).setHeader("Trạng thái").setAutoWidth(true);

        grid.addColumn(bc -> {
            BigDecimal price = (bc.getBook() != null && bc.getBook().getPrice() != null)
                    ? bc.getBook().getPrice() : bc.getAcquisitionPrice();
            return price != null ? currencyFormat.format(price) + " đ" : "-";
        }).setHeader("Giá bìa").setAutoWidth(true);

        grid.addComponentColumn(copy -> {
            Button restoreBtn = new Button("Phục hồi sách", VaadinIcon.REFRESH.create());
            restoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            restoreBtn.addClickListener(e -> showRestoreBookDialog(copy));
            return restoreBtn;
        }).setHeader("Thao tác").setAutoWidth(true);

        Runnable dataLoader = () -> {
            String kw = searchField.getValue() != null ? searchField.getValue().trim() : "";
            List<BookCopy> copies = dataManager.load(BookCopy.class)
                    .query("select bc from BookCopy bc where bc.deletedAt is null " +
                            "and bc.status in ('LOST', 'DAMAGED') " +
                            "and (:kw = '' or lower(bc.barcode) like lower(concat('%', :kw, '%')) " +
                            "or lower(bc.book.title) like lower(concat('%', :kw, '%'))) " +
                            "order by bc.updatedAt desc")
                    .parameter("kw", kw)
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                            .add("book", bookPlan -> bookPlan.addFetchPlan(FetchPlan.BASE).add("publisher", FetchPlan.BASE)))
                    .list();
            grid.setItems(copies);
        };

        searchField.addValueChangeListener(e -> {
            currentSearchKeyword = e.getValue();
            dataLoader.run();
        });

        dataLoader.run();
        layout.add(grid);
        restoreBookTabContent.add(layout);
    }

    private void showRestoreBookDialog(BookCopy copy) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Phục hồi sách báo mất tìm lại được");
        dialog.setWidth("500px");

        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);

        String bookTitle = copy.getBook() != null ? copy.getBook().getTitle() : "";

        Div infoBox = new Div();
        infoBox.addClassNames("p-s", "bg-success-10", "rounded-m", "w-full", "text-s", "text-success");
        infoBox.add(new Span(String.format("Sách: '%s' (Mã vạch: %s) sẽ được chuyển về trạng thái Sẵn sàng (AVAILABLE). Các khoản phạt mất sách chưa thanh toán liên quan sẽ được tự động miễn giảm.",
                bookTitle, copy.getBarcode())));
        form.add(infoBox);

        TextArea noteField = new TextArea("Ghi chú kiểm toán");
        noteField.setPlaceholder("VD: Độc giả tìm thấy và đem trả lại, thủ thư kiểm kê lại kho tìm thấy...");
        noteField.setWidthFull();
        noteField.setValue("Độc giả đã tìm lại được sách và hoàn trả thư viện");
        form.add(noteField);

        Button confirmBtn = new Button("Xác nhận phục hồi", VaadinIcon.CHECK.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        confirmBtn.addClickListener(e -> {
            try {
                BookCopy reloaded = dataManager.load(BookCopy.class).id(copy.getId()).one();
                reloaded.setStatus(BookCopyStatus.AVAILABLE.getId());
                reloaded.setCondition(BookCopyCondition.GOOD.getId());
                dataManager.save(reloaded);

                // Auto cancel any unpaid LOST fine for this book copy if existing
                List<Fine> relatedFines = dataManager.load(Fine.class)
                        .query("select f from Fine f where f.loanItem.bookCopy.id = :copyId and f.status in ('UNPAID', 'PARTIAL') and f.fineType = 'LOST'")
                        .parameter("copyId", copy.getId())
                        .list();

                for (Fine f : relatedFines) {
                    f.setStatus(FineStatus.WAIVED.getId());
                    f.setWaivedReason("Tự động miễn phạt do sách đã được phục hồi: " + noteField.getValue());
                    dataManager.save(f);
                }

                auditLogService.log("RESTORE_LOST_BOOK", "BookCopy", copy.getBarcode(),
                        String.format("Phục hồi sách '%s' (Mã vạch %s) về trạng thái AVAILABLE. Ghi chú: %s",
                                bookTitle, copy.getBarcode(), noteField.getValue()));

                toastService.success("Đã phục hồi sách " + copy.getBarcode() + " về kho sẵn sàng!");
                dialog.close();
                renderRestoreLostBooksTab();
            } catch (Exception ex) {
                toastService.error("Lỗi khi phục hồi sách: " + ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Đóng", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }
}
