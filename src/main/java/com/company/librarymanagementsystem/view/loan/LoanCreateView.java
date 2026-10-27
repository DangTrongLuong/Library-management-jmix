package com.company.librarymanagementsystem.view.loan;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.LoanService;
import com.company.librarymanagementsystem.service.MemberService;
import com.company.librarymanagementsystem.service.ReservationService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "loans/create", layout = MainView.class)
@ViewController("LoanCreate.view")
@ViewDescriptor("loan-create-view.xml")
public class LoanCreateView extends StandardView implements BeforeEnterObserver {

    @ViewComponent
    private CollectionContainer<BookCopy> selectedCopiesDc;

    @ViewComponent
    private VerticalLayout mainContainer;
    @ViewComponent
    private ComboBox<Member> memberComboBox;
    @ViewComponent
    private Button recheckEligibilityBtn;
    @ViewComponent
    private Div eligibilityCard;

    @ViewComponent
    private TextField barcodeField;
    @ViewComponent
    private Button addBookBtn;
    @ViewComponent
    private DataGrid<BookCopy> selectedCopiesGrid;

    @ViewComponent
    private TextField noteField;
    @ViewComponent
    private Button submitBtn;
    @ViewComponent
    private Button cancelBtn;
    @ViewComponent
    private Button cancelTopBtn;

    @Autowired
    private LoanService loanService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private ReservationService reservationService;

    private MemberService.EligibilityResult currentEligibility;

    @Subscribe
    public void onInit(InitEvent event) {
        getContent().setAlignItems(FlexComponent.Alignment.CENTER);
        getContent().setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        getContent().setWidthFull();
        if (mainContainer != null) {
            mainContainer.getStyle()
                    .set("margin-left", "auto")
                    .set("margin-right", "auto")
                    .set("align-self", "center");
        }
        initMemberSelector();
        initBookCopyGrid();
        initActions();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parameters = queryParameters.getParameters();

        List<String> memberIdParam = parameters.get("memberId");
        if (memberIdParam != null && !memberIdParam.isEmpty()) {
            try {
                Long mId = Long.parseLong(memberIdParam.get(0));
                for (Member m : memberComboBox.getListDataView().getItems().toList()) {
                    if (m.getId().equals(mId)) {
                        memberComboBox.setValue(m);
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        List<String> barcodeParam = parameters.get("barcode");
        if (barcodeParam != null && !barcodeParam.isEmpty()) {
            String barcode = barcodeParam.get(0);
            barcodeField.setValue(barcode);
            addCopyByBarcode();
        }
    }

    private void initMemberSelector() {
        List<Member> members = dataManager.load(Member.class)
                .query("select m from LibraryMember m order by m.fullName")
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("memberType", FetchPlan.BASE)
                        .add("user", FetchPlan.BASE))
                .list();

        memberComboBox.setItems(members);
        memberComboBox.setItemLabelGenerator(m -> String.format("[%s] %s (%s)",
                m.getMemberCode(), m.getFullName(),
                m.getMemberType() != null ? m.getMemberType().getName() : ""));

        memberComboBox.addValueChangeListener(e -> {
            Member selected = e.getValue();
            if (selected != null) {
                checkAndRenderEligibility(selected);
            } else {
                eligibilityCard.setVisible(false);
                currentEligibility = null;
            }
            selectedCopiesGrid.getDataCommunicator().reset();
        });

        recheckEligibilityBtn.addClickListener(e -> {
            Member selected = memberComboBox.getValue();
            if (selected != null) {
                checkAndRenderEligibility(selected);
                toastService.info("Đã cập nhật lại tư cách mượn của độc giả!");
            }
        });
    }

    private void checkAndRenderEligibility(Member member) {
        currentEligibility = memberService.checkEligibility(member);
        eligibilityCard.setVisible(true);
        eligibilityCard.removeAll();

        boolean ok = currentEligibility.isEligible();
        String bgColor = ok ? "#f0fdf4" : "#fef2f2";
        String borderColor = ok ? "#86efac" : "#fca5a5";
        String textColor = ok ? "#166534" : "#991b1b";

        eligibilityCard.getStyle()
                .set("background", bgColor)
                .set("border", "1px solid " + borderColor)
                .set("border-radius", "8px")
                .set("padding", "6px 12px")
                .set("margin-top", "8px")
                .set("display", "inline-flex")
                .set("flex-direction", "column")
                .set("width", "fit-content")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        HorizontalLayout contentRow = new HorizontalLayout();
        contentRow.setAlignItems(FlexComponent.Alignment.CENTER);
        contentRow.setSpacing(true);
        contentRow.getStyle().set("flex-wrap", "wrap").set("gap", "6px");

        Span badge = new Span(ok ? "ĐỦ ĐIỀU KIỆN" : "KHÔNG ĐỦ ĐIỀU KIỆN");
        badge.getStyle()
                .set("font-weight", "700")
                .set("font-size", "13px")
                .set("color", textColor)
                .set("white-space", "nowrap")
                .set("margin-right", "4px");
        contentRow.add(badge);

        Span m1 = createMetricBadge("Thẻ", currentEligibility.getStatus(), ok ? "#16a34a" : "#dc2626");
        Span m2 = createMetricBadge("Đang mượn", String.format("%d / %d cuốn", currentEligibility.getCurrentLoans(),
                currentEligibility.getMaxLoanBooks()), "#2563eb");
        Span m3 = createMetricBadge("Quá hạn", currentEligibility.getOverdueLoans() + " cuốn",
                currentEligibility.getOverdueLoans() > 0 ? "#dc2626" : "#475569");
        Span m4 = createMetricBadge("Nợ phạt", String.format("%,.0f VNĐ", currentEligibility.getUnpaidFines()),
                currentEligibility.getUnpaidFines().compareTo(BigDecimal.ZERO) > 0 ? "#dc2626" : "#475569");

        contentRow.add(m1, m2, m3, m4);
        eligibilityCard.add(contentRow);

        if (!ok && !currentEligibility.getBlockingReasons().isEmpty()) {
            VerticalLayout reasonsBox = new VerticalLayout();
            reasonsBox.setPadding(false);
            reasonsBox.setSpacing(false);
            reasonsBox.getStyle().set("margin-top", "6px");

            for (String r : currentEligibility.getBlockingReasons()) {
                Span reasonItem = new Span("• " + r);
                reasonItem.getStyle().set("color", "#b91c1c").set("font-size", "12px").set("font-weight", "500");
                reasonsBox.add(reasonItem);
            }
            eligibilityCard.add(reasonsBox);
        }
    }

    private Span createMetricBadge(String label, String value, String color) {
        Span span = new Span(label + ": " + value);
        span.getStyle()
                .set("background", "white")
                .set("padding", "2px 7px")
                .set("border-radius", "4px")
                .set("border", "1px solid #e2e8f0")
                .set("font-weight", "600")
                .set("font-size", "12px")
                .set("white-space", "nowrap")
                .set("color", color);
        return span;
    }

    private void initBookCopyGrid() {
        selectedCopiesGrid.getColumnByKey("author").setRenderer(new ComponentRenderer<>(bc -> {
            if (bc.getBook() != null && bc.getBook().getAuthors() != null) {
                return new Span(bc.getBook().getAuthors().stream()
                        .map(Author::getFullName)
                        .collect(Collectors.joining(", ")));
            }
            return new Span("-");
        }));

        selectedCopiesGrid.getColumnByKey("dueDate").setRenderer(new ComponentRenderer<>(bc -> {
            Member m = memberComboBox.getValue();
            int days = (m != null && m.getMemberType() != null && m.getMemberType().getLoanDays() != null)
                    ? m.getMemberType().getLoanDays()
                    : 14;
            return new Span(LocalDate.now().plusDays(days).toString());
        }));

        selectedCopiesGrid.getColumnByKey("actions").setRenderer(new ComponentRenderer<>(bc -> {
            Button delBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                selectedCopiesDc.getMutableItems().remove(bc);
            });
            delBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return delBtn;
        }));
    }

    private void initActions() {
        addBookBtn.addClickListener(e -> addCopyByBarcode());
        barcodeField.addKeyDownListener(com.vaadin.flow.component.Key.ENTER, e -> addCopyByBarcode());

        submitBtn.addClickListener(this::onSubmitLoan);
        cancelBtn.addClickListener(e -> backToList());
        cancelTopBtn.addClickListener(e -> backToList());
    }

    private void addCopyByBarcode() {
        String barcode = barcodeField.getValue();
        if (barcode == null || barcode.trim().isEmpty()) {
            toastService.warning("Vui lòng nhập hoặc quét mã vạch!");
            return;
        }

        Member member = memberComboBox.getValue();
        if (member == null) {
            toastService.warning("Vui lòng chọn độc giả trước khi quét sách!");
            return;
        }

        if (currentEligibility != null && !currentEligibility.isEligible()) {
            toastService.error("Độc giả không đủ điều kiện mượn sách! Vui lòng kiểm tra lại lý do chặn.");
            return;
        }

        int maxLoans = currentEligibility != null ? currentEligibility.getMaxLoanBooks() : 5;
        int currentLoans = currentEligibility != null ? currentEligibility.getCurrentLoans() : 0;
        int inList = selectedCopiesDc.getItems().size();

        if (currentLoans + inList + 1 > maxLoans) {
            toastService.warning(String.format(
                    "Không thể thêm! Độc giả chỉ được mượn tối đa %d cuốn (Đang mượn: %d, Trong phiếu: %d).",
                    maxLoans, currentLoans, inList));
            return;
        }

        // Kiểm tra xem đã có trong danh sách đang chọn chưa
        boolean alreadyAdded = selectedCopiesDc.getItems().stream()
                .anyMatch(c -> barcode.trim().equalsIgnoreCase(c.getBarcode()));
        if (alreadyAdded) {
            toastService.warning("Cuốn sách mã vạch này đã có trong danh sách mượn!");
            barcodeField.clear();
            return;
        }

        Optional<BookCopy> opt = dataManager.load(BookCopy.class)
                .query("select bc from BookCopy bc where bc.barcode = :barcode")
                .parameter("barcode", barcode.trim())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("book", bookPlan -> bookPlan.addFetchPlan(FetchPlan.BASE).add("authors", FetchPlan.BASE))
                        .add("location", FetchPlan.BASE))
                .optional();

        if (opt.isEmpty()) {
            toastService.error("Không tìm thấy bản sao sách nào với mã vạch: " + barcode);
            return;
        }

        BookCopy copy = opt.get();

        String reservedOther = reservationService.checkReservedForOtherMember(copy, member);
        if (reservedOther != null) {
            toastService.error(String.format("Bản sao '%s' đang được giữ chỗ cho %s!", copy.getBarcode(), reservedOther));
            return;
        }

        if (!BookCopyStatus.AVAILABLE.getId().equalsIgnoreCase(copy.getStatus())
                && !BookCopyStatus.RESERVED.getId().equalsIgnoreCase(copy.getStatus())) {
            toastService.error(String.format("Bản sao '%s' không ở trạng thái sẵn sàng (Hiện tại: %s)!",
                    copy.getBarcode(), copy.getStatus()));
            return;
        }

        if (copy.getBook() != null && Boolean.TRUE.equals(copy.getBook().getIsReference())) {
            toastService.error(String.format("Sách '%s' là sách tham khảo, chỉ được đọc tại chỗ!",
                    copy.getBook().getTitle()));
            return;
        }

        selectedCopiesDc.getMutableItems().add(copy);
        barcodeField.clear();
        toastService.success(String.format("Đã thêm: %s (%s)", copy.getBook() != null ? copy.getBook().getTitle() : "",
                copy.getBarcode()));
    }

    private void onSubmitLoan(ClickEvent<Button> event) {
        Member member = memberComboBox.getValue();
        if (member == null) {
            toastService.warning("Vui lòng chọn độc giả!");
            return;
        }

        List<BookCopy> copies = new ArrayList<>(selectedCopiesDc.getItems());
        if (copies.isEmpty()) {
            toastService.warning("Vui lòng quét hoặc thêm ít nhất một cuốn sách vào phiếu!");
            return;
        }

        if (currentEligibility != null && !currentEligibility.isEligible()) {
            toastService.error("Độc giả không đủ điều kiện mượn sách!");
            return;
        }

        try {
            User currentUser = (User) currentAuthentication.getUser();
            Loan loan = loanService.createLoan(member, copies, currentUser, noteField.getValue());
            toastService.success(String.format("Lập phiếu mượn %s thành công (%d cuốn)!",
                    loan.getLoanCode(), copies.size()));
            backToList();
        } catch (Exception ex) {
            toastService.error("Lỗi khi lập phiếu mượn: " + ex.getMessage());
        }
    }

    private void backToList() {
        viewNavigators.view(this, LoanListView.class).navigate();
    }
}
