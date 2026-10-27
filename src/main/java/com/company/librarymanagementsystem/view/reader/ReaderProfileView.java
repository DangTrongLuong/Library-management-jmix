package com.company.librarymanagementsystem.view.reader;

import com.company.librarymanagementsystem.entity.Member;
import com.company.librarymanagementsystem.entity.MemberStatus;
import com.company.librarymanagementsystem.entity.MemberType;
import com.company.librarymanagementsystem.entity.User;
import com.company.librarymanagementsystem.service.MemberService;
import com.company.librarymanagementsystem.service.ReaderLoanService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Route(value = "reader-profile", layout = MainView.class)
@ViewController("ReaderProfileView")
@ViewDescriptor("reader-profile-view.xml")
public class ReaderProfileView extends StandardView {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @ViewComponent
    private VerticalLayout linkedMemberContainer;
    @ViewComponent
    private Div cardAndDetailsWrapper;

    @ViewComponent
    private VerticalLayout unlinkedMemberContainer;
    @ViewComponent
    private Div unlinkedIntroBanner;
    @ViewComponent
    private TextField searchMemberCodeField;
    @ViewComponent
    private TextField searchIdNumberField;
    @ViewComponent
    private Button searchCardBtn;
    @ViewComponent
    private Button clearSearchBtn;
    @ViewComponent
    private Div searchResultWrapper;

    @ViewComponent
    private Button refreshBtn;

    @Autowired
    private MemberService memberService;
    @Autowired
    private ReaderLoanService readerLoanService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    private User currentUser;
    private Member currentMember;

    @Subscribe
    public void onInit(InitEvent event) {
        searchCardBtn.addClickListener(this::onSearchCardClick);
        clearSearchBtn.addClickListener(e -> {
            searchMemberCodeField.clear();
            searchIdNumberField.clear();
            searchResultWrapper.removeAll();
        });
        refreshBtn.addClickListener(e -> loadMemberData());

        searchMemberCodeField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> onSearchCardClick(null));
        searchIdNumberField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> onSearchCardClick(null));
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        loadMemberData();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage().addStyleSheet(
                "https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700;800&display=swap");
    }

    private void loadMemberData() {
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User u) {
            this.currentUser = u;
        } else {
            this.currentUser = null;
        }

        Optional<Member> memberOpt = readerLoanService.getCurrentMember();
        if (memberOpt.isPresent()) {
            this.currentMember = memberOpt.get();
            linkedMemberContainer.setVisible(true);
            unlinkedMemberContainer.setVisible(false);
            renderLinkedMemberView(this.currentMember);
        } else {
            this.currentMember = null;
            linkedMemberContainer.setVisible(false);
            unlinkedMemberContainer.setVisible(true);
            renderUnlinkedIntro();
            searchResultWrapper.removeAll();
        }
    }

    // =========================================================================
    // TRƯỜNG HỢP 1: ĐÃ LIÊN KẾT THẺ (Digital Card & Profile Details)
    // =========================================================================
    private void renderLinkedMemberView(Member member) {
        cardAndDetailsWrapper.removeAll();

        // Banner thông báo nếu hồ sơ thẻ đang chờ thủ thư cấp thẻ chính thức tại quầy
        if (MemberStatus.PENDING.getId().equalsIgnoreCase(member.getStatus())) {
            Div pendingBanner = new Div();
            pendingBanner.addClassNames("p-m", "rounded-m", "border", "border-warning-50", "bg-warning-10", "text-warning", "flex", "items-center", "gap-m", "w-full", "mb-m");
            pendingBanner.getStyle().set("line-height", "1.5");
            Span alertIcon = new Span(VaadinIcon.EXCLAMATION_CIRCLE.create());
            alertIcon.getStyle().set("font-size", "24px").set("flex-shrink", "0");
            Span alertText = new Span("Thẻ thư viện của bạn hiện đang ở trạng thái CHỜ DUYỆT CẤP THẺ. Vui lòng mang theo giấy tờ chứng minh đối tượng (thẻ sinh viên, thẻ cán bộ, giấy tờ liên quan...) đến quầy thư viện để Thủ thư đối chiếu và kích hoạt thẻ chính thức trước khi mượn sách.");
            pendingBanner.add(alertIcon, alertText);
            cardAndDetailsWrapper.add(pendingBanner);
        }

        HorizontalLayout mainRow = new HorizontalLayout();
        mainRow.setWidthFull();
        mainRow.setSpacing(true);
        mainRow.setAlignItems(FlexComponent.Alignment.START);
        mainRow.addClassNames("flex-wrap", "gap-m", "box-border");

        // CỘT 1: THẺ THƯ VIỆN ĐIỆN TỬ (DIGITAL LIBRARY CARD)
        Div cardWrapper = new Div();
        cardWrapper.addClassNames("flex", "flex-col", "gap-s", "box-border");
        cardWrapper.getStyle().set("flex", "1 1 380px").set("max-width", "440px");

        Div digitalCard = createDigitalCard(member);
        cardWrapper.add(digitalCard);

        // CỘT 2: THÔNG TIN CHI TIẾT & QUYỀN LỢI THẺ
        Div detailsWrapper = new Div();
        detailsWrapper.addClassNames("flex", "flex-col", "gap-m", "box-border");
        detailsWrapper.getStyle().set("flex", "2 1 420px").set("min-width", "320px");

        detailsWrapper.add(createDetailsPanel(member));
        detailsWrapper.add(createPrivilegesPanel(member));

        mainRow.add(cardWrapper, detailsWrapper);
        cardAndDetailsWrapper.add(mainRow);
    }

    /**
     * Tạo Thẻ Thư Viện Điện Tử (Digital Card) sang trọng với gradient Xanh da trời & mã vạch
     */
    private Div createDigitalCard(Member member) {
        Div card = new Div();
        card.addClassNames("p-m", "box-border");
        card.getStyle()
                .set("background", "linear-gradient(135deg, #0ea5e9 0%, #0284c7 45%, #0369a1 100%)")
                .set("font-family", "'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif")
                .set("position", "relative")
                .set("overflow", "hidden")
                .set("border-radius", "10px")
                .set("border", "1px solid rgba(255, 255, 255, 0.28)")
                .set("box-shadow", "0 14px 28px -4px rgba(3, 105, 161, 0.35)")
                .set("min-height", "280px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "space-between");

        // Background decorative circles (Canva aesthetic glare)
        Div circle1 = new Div();
        circle1.getStyle()
                .set("position", "absolute")
                .set("top", "-40px")
                .set("right", "-40px")
                .set("width", "160px")
                .set("height", "160px")
                .set("border-radius", "50%")
                .set("background", "radial-gradient(circle, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0) 70%)")
                .set("pointer-events", "none");

        Div circle2 = new Div();
        circle2.getStyle()
                .set("position", "absolute")
                .set("bottom", "-50px")
                .set("left", "-30px")
                .set("width", "180px")
                .set("height", "180px")
                .set("border-radius", "50%")
                .set("background", "radial-gradient(circle, rgba(56,189,248,0.2) 0%, rgba(255,255,255,0) 70%)")
                .set("pointer-events", "none");

        card.add(circle1, circle2);

        // --- TOP: LOGO (NHƯ TRANG LOGIN) & CHIP ---
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout branding = new HorizontalLayout();
        branding.setSpacing(true);
        branding.setAlignItems(FlexComponent.Alignment.CENTER);

        // Logo từ trang đăng nhập (login-logo.png) trong khung nền trắng bo góc sang
        // trọng
        Image logoImg = new Image("public/images/login-logo.png", "Logo Thư Viện");
        logoImg.setWidth("34px");
        logoImg.setHeight("34px");
        logoImg.getStyle().set("object-fit", "contain");

        Div logoBox = new Div(logoImg);
        logoBox.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "6px")
                .set("padding", "2px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "36px")
                .set("height", "36px")
                .set("box-shadow", "0 2px 5px rgba(0,0,0,0.2)")
                .set("flex-shrink", "0");

        VerticalLayout brandText = new VerticalLayout();
        brandText.setPadding(false);
        brandText.setSpacing(false);
        Span brandTitle = new Span("HỆ THỐNG THƯ VIỆN ĐIỆN TỬ");
        brandTitle.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "11px")
                .set("font-weight", "700")
                .set("letter-spacing", "1px")
                .set("color", "#ffffff")
                .set("text-shadow", "0 1px 2px rgba(0,0,0,0.25)");

        Span brandSub = new Span("THẺ ĐỘC GIẢ SỐ • DIGITAL LIBRARY CARD");
        brandSub.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "9px")
                .set("font-weight", "500")
                .set("color", "rgba(255, 255, 255, 0.85)")
                .set("letter-spacing", "0.5px");
        brandText.add(brandTitle, brandSub);
        branding.add(logoBox, brandText);

        // Simulating Smart Card Gold Chip
        Div goldChip = new Div();
        goldChip.getStyle()
                .set("width", "38px")
                .set("height", "28px")
                .set("background", "linear-gradient(135deg, #fef08a 0%, #eab308 50%, #ca8a04 100%)")
                .set("border-radius", "4px")
                .set("border", "1px solid #d97706")
                .set("box-shadow", "inset 0 1px 1px rgba(255,255,255,0.8), 0 2px 4px rgba(0,0,0,0.25)");

        topRow.add(branding, goldChip);

        // --- MIDDLE: CÁC THÔNG TIN CÓ TIÊU ĐỀ RÕ RÀNG & DÃN ĐỀU NHAU ---
        VerticalLayout midSection = new VerticalLayout();
        midSection.setPadding(false);
        midSection.setSpacing(false);
        midSection.addClassNames("my-xs");

        // 1. Khối Mã thẻ thư viện
        Div codeBox = new Div();
        codeBox.addClassNames("flex", "flex-col");
        Span codeLabel = new Span("MÃ SỐ THẺ");
        codeLabel.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "9.5px")
                .set("font-weight", "600")
                .set("letter-spacing", "1px")
                .set("color", "rgba(255, 255, 255, 0.75)")
                .set("text-transform", "uppercase");

        Span memberCodeSpan = new Span(member.getMemberCode());
        memberCodeSpan.getStyle()
                .set("font-family", "'Poppins', monospace")
                .set("font-size", "21px")
                .set("font-weight", "800")
                .set("letter-spacing", "2.5px")
                .set("color", "#ffffff")
                .set("text-shadow", "0 2px 4px rgba(0,0,0,0.2)");
        codeBox.add(codeLabel, memberCodeSpan);

        // 2. Khối Họ và tên & CCCD dãn đều 2 bên
        HorizontalLayout infoRow = new HorizontalLayout();
        infoRow.setWidthFull();
        infoRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        infoRow.setAlignItems(FlexComponent.Alignment.END);
        infoRow.getStyle().set("margin-top", "8px");

        // Cột Họ và tên
        Div nameBox = new Div();
        nameBox.addClassNames("flex", "flex-col");
        Span nameLabel = new Span("HỌ VÀ TÊN");
        nameLabel.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "9.5px")
                .set("font-weight", "600")
                .set("letter-spacing", "0.8px")
                .set("color", "rgba(255, 255, 255, 0.75)")
                .set("text-transform", "uppercase");

        String resolvedName = member.getFullName();
        if ((resolvedName == null || resolvedName.isBlank() || "Reader".equalsIgnoreCase(resolvedName))
                && currentUser != null && currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) {
            resolvedName = currentUser.getFullName();
        }
        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = "ĐỘC GIẢ";
        }

        Span fullNameSpan = new Span(resolvedName.toUpperCase());
        fullNameSpan.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "15px")
                .set("font-weight", "700")
                .set("color", "#ffffff")
                .set("letter-spacing", "0.5px")
                .set("text-shadow", "0 1px 2px rgba(0,0,0,0.2)");
        nameBox.add(nameLabel, fullNameSpan);

        // Cột Đối tượng độc giả
        Div typeBox = new Div();
        typeBox.addClassNames("flex", "flex-col");
        Span typeLabel = new Span("ĐỐI TƯỢNG ĐỘC GIẢ");
        typeLabel.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "9.5px")
                .set("font-weight", "600")
                .set("letter-spacing", "0.8px")
                .set("color", "rgba(255, 255, 255, 0.75)")
                .set("text-transform", "uppercase");

        String typeVal = (member.getMemberType() != null && member.getMemberType().getName() != null)
                ? member.getMemberType().getName() : "Độc giả";
        Span typeSpan = new Span(typeVal.toUpperCase());
        typeSpan.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "13.5px")
                .set("font-weight", "600")
                .set("color", "#ffffff")
                .set("letter-spacing", "0.5px");
        typeBox.add(typeLabel, typeSpan);

        infoRow.add(nameBox, typeBox);

        midSection.add(codeBox, infoRow);

        // --- BOTTOM: DATES (2 DÒNG RIÊNG) & TRẠNG THÁI (MÀU NHẠT, KHÔNG XUỐNG DÒNG) & BARCODE ---
        VerticalLayout bottomSection = new VerticalLayout();
        bottomSection.setPadding(false);
        bottomSection.setSpacing(false);

        HorizontalLayout validityRow = new HorizontalLayout();
        validityRow.setWidthFull();
        validityRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        validityRow.setAlignItems(FlexComponent.Alignment.CENTER);
        validityRow.addClassNames("mb-xs");

        String issued = member.getCardIssuedDate() != null ? member.getCardIssuedDate().format(DATE_FMT) : "-";
        String expiry = member.getCardExpiryDate() != null ? member.getCardExpiryDate().format(DATE_FMT) : "-";

        VerticalLayout dateBox = new VerticalLayout();
        dateBox.setPadding(false);
        dateBox.setSpacing(false);

        Span issuedSpan = new Span("Ngày cấp: " + issued);
        issuedSpan.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "11px")
                .set("font-weight", "500")
                .set("color", "rgba(255, 255, 255, 0.9)")
                .set("line-height", "1.4");

        Span expirySpan = new Span("Hạn thẻ:  " + expiry);
        expirySpan.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "11px")
                .set("font-weight", "500")
                .set("color", "rgba(255, 255, 255, 0.9)")
                .set("line-height", "1.4");

        dateBox.add(issuedSpan, expirySpan);

        // Huy hiệu trạng thái: Màu nhạt (soft pastel), 1 dòng đều, không chữ trên chữ dưới
        Span statusPill = new Span();
        statusPill.getStyle()
                .set("font-family", "'Poppins', sans-serif")
                .set("font-size", "11px")
                .set("font-weight", "600")
                .set("padding", "4px 10px")
                .set("border-radius", "6px")
                .set("white-space", "nowrap")
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("flex-shrink", "0")
                .set("letter-spacing", "0.5px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)");

        if (MemberStatus.ACTIVE.getId().equalsIgnoreCase(member.getStatus())) {
            statusPill.setText("● HOẠT ĐỘNG");
            statusPill.getStyle()
                    .set("background", "rgba(16, 185, 129, 0.22)")
                    .set("border", "1px solid rgba(167, 243, 208, 0.45)")
                    .set("color", "#ffffff");
        } else if (MemberStatus.PENDING.getId().equalsIgnoreCase(member.getStatus())) {
            statusPill.setText("● CHỜ DUYỆT CẤP THẺ");
            statusPill.getStyle()
                    .set("background", "rgba(245, 158, 11, 0.35)")
                    .set("border", "1px solid rgba(253, 230, 138, 0.7)")
                    .set("color", "#fef08a");
        } else if (MemberStatus.EXPIRED.getId().equalsIgnoreCase(member.getStatus())) {
            statusPill.setText("● HẾT HẠN");
            statusPill.getStyle()
                    .set("background", "rgba(239, 68, 68, 0.22)")
                    .set("border", "1px solid rgba(254, 202, 202, 0.45)")
                    .set("color", "#ffffff");
        } else {
            statusPill.setText("● " + (member.getStatus() != null ? member.getStatus().toUpperCase() : "KHÓA"));
            statusPill.getStyle()
                    .set("background", "rgba(239, 68, 68, 0.22)")
                    .set("border", "1px solid rgba(254, 202, 202, 0.45)")
                    .set("color", "#ffffff");
        }

        validityRow.add(dateBox, statusPill);

        // Visual Barcode strip on card footer (for easy barcode scanning at library counter)
        Div barcodeBox = createSimulatedBarcode(member.getMemberCode());

        bottomSection.add(validityRow, barcodeBox);

        card.add(topRow, midSection, bottomSection);
        return card;
    }

    /**
     * Tạo dải mã vạch mô phỏng chân thực phục vụ quét tại quầy
     */
    private Div createSimulatedBarcode(String code) {
        Div wrapper = new Div();
        wrapper.addClassNames("px-s", "py-xs", "box-border");
        wrapper.getStyle()
                .set("background", "rgba(255, 255, 255, 0.95)")
                .set("border-radius", "6px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.15)");

        // Bars container
        Div bars = new Div();
        bars.getStyle()
                .set("display", "flex")
                .set("align-items", "stretch")
                .set("height", "28px")
                .set("gap", "2px")
                .set("width", "92%");

        int[] widths = { 2, 1, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 4, 1, 2, 3, 1, 4, 2, 1, 3, 1, 2 };
        for (int i = 0; i < widths.length; i++) {
            Div bar = new Div();
            bar.getStyle()
                    .set("background", (i % 2 == 0) ? "#0f172a" : "transparent")
                    .set("width", widths[i] + "px")
                    .set("height", "100%");
            bars.add(bar);
        }

        Span codeText = new Span(code);
        codeText.getStyle()
                .set("font-family", "'Poppins', monospace")
                .set("font-size", "11px")
                .set("font-weight", "700")
                .set("color", "#0f172a")
                .set("letter-spacing", "2px")
                .set("margin-top", "2px");

        wrapper.add(bars, codeText);
        return wrapper;
    }

    /**
     * Khung chi tiết hồ sơ cá nhân
     */
    private Div createDetailsPanel(Member member) {
        Div panel = new Div();
        panel.addClassNames("bg-base", "rounded-m", "border", "border-contrast-10", "p-m", "shadow-xs", "box-border");

        H4 title = new H4("Thông Tin Hồ Sơ Độc Giả");
        title.addClassNames("text-primary", "mb-s", "font-bold");
        title.getStyle().set("margin-top", "0");

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
                .set("gap", "12px");

        String genderText = "Chưa rõ";
        if (member.getGender() != null) {
            if ("MALE".equalsIgnoreCase(member.getGender())) {
                genderText = "Nam";
            } else if ("FEMALE".equalsIgnoreCase(member.getGender())) {
                genderText = "Nữ";
            } else {
                genderText = member.getGender();
            }
        }

        String resolvedName = member.getFullName();
        if ((resolvedName == null || resolvedName.isBlank() || "Reader".equalsIgnoreCase(resolvedName))
                && currentUser != null && currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) {
            resolvedName = currentUser.getFullName();
        }

        grid.add(
                createDetailItem("Tài khoản liên kết", currentUser != null ? "@" + currentUser.getUsername() : "-",
                        "vaadin:user"),
                createDetailItem("Họ và tên", resolvedName != null ? resolvedName : "-", "vaadin:id-card"),
                createDetailItem("Mã thẻ thư viện", member.getMemberCode(), "vaadin:barcode"),
                createDetailItem("Số CCCD / CMND",
                        member.getIdNumber() != null ? member.getIdNumber() : "Chưa cập nhật", "vaadin:diploma"),
                createDetailItem("Loại độc giả",
                        member.getMemberType() != null ? member.getMemberType().getName() : "-", "vaadin:tag"),
                createDetailItem("Ngày sinh",
                        member.getDateOfBirth() != null ? member.getDateOfBirth().format(DATE_FMT) : "Chưa rõ",
                        "vaadin:calendar"),
                createDetailItem("Giới tính", genderText, "vaadin:users"),
                createDetailItem("Địa chỉ", member.getAddress() != null ? member.getAddress() : "Chưa cập nhật",
                        "vaadin:home"));

        // Nút Hủy liên kết thẻ
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.addClassNames("mt-m", "pt-s", "border-t", "border-contrast-10");

        Button unlinkBtn = new Button("Hủy liên kết thẻ này", VaadinIcon.UNLINK.create(),
                e -> onUnlinkCardClick(member));
        unlinkBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        actions.add(unlinkBtn);

        panel.add(title, grid, actions);
        return panel;
    }

    /**
     * Khung quyền lợi thẻ thư viện
     */
    private Div createPrivilegesPanel(Member member) {
        Div panel = new Div();
        panel.addClassNames("bg-base", "rounded-m", "border", "border-contrast-10", "p-m", "shadow-xs", "box-border");

        H4 title = new H4("Chính Sách & Hạn Mức Mượn Sách");
        title.addClassNames("text-primary", "mb-s", "font-bold");
        title.getStyle().set("margin-top", "0");

        MemberType type = member.getMemberType();
        int maxBooks = (type != null && type.getMaxLoanBooks() != null) ? type.getMaxLoanBooks() : 5;
        int loanDays = (type != null && type.getLoanDays() != null) ? type.getLoanDays() : 14;
        int maxRenew = (type != null && type.getMaxRenewals() != null) ? type.getMaxRenewals() : 1;

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(180px, 1fr))")
                .set("gap", "10px");

        grid.add(
                createPrivilegeCard("Số sách mượn cùng lúc", maxBooks + " cuốn",
                        "bg-primary-10 text-primary border-primary-20"),
                createPrivilegeCard("Thời hạn mỗi lần mượn", loanDays + " ngày",
                        "bg-success-10 text-success border-success-20"),
                createPrivilegeCard("Số lần gia hạn sách", maxRenew + " lần/cuốn",
                        "bg-warning-10 text-warning border-warning-20"));

        panel.add(title, grid);
        return panel;
    }

    private Div createDetailItem(String label, String value, String iconName) {
        Div item = new Div();
        item.addClassNames("flex", "flex-col", "gap-xs", "box-border");

        Span lbl = new Span(label);
        lbl.addClassNames("text-secondary", "text-xs", "font-medium");

        Span val = new Span(value != null && !value.isBlank() ? value : "-");
        val.addClassNames("text-primary", "text-s", "font-bold");

        item.add(lbl, val);
        return item;
    }

    private Div createPrivilegeCard(String label, String value, String colorClasses) {
        Div card = new Div();
        card.addClassNames("rounded-m", "p-s", "border", "box-border", "flex", "flex-col", "justify-between");
        for (String c : colorClasses.split(" ")) {
            if (!c.isBlank())
                card.addClassName(c);
        }

        Span lbl = new Span(label);
        lbl.addClassNames("text-xs", "font-medium");

        Span val = new Span(value);
        val.addClassNames("text-m", "font-bold", "mt-xs");

        card.add(lbl, val);
        return card;
    }

    private void onUnlinkCardClick(Member member) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Xác nhận hủy liên kết thẻ");
        dialog.setText(String.format(
                "Bạn có chắc chắn muốn hủy liên kết thẻ thư viện %s khỏi tài khoản '%s' không? Sau khi hủy, bạn sẽ không thể thực hiện các thao tác mượn, gia hạn hay đặt trước sách cho đến khi liên kết lại.",
                member.getMemberCode(), currentUser != null ? currentUser.getUsername() : ""));
        dialog.setCancelable(true);
        dialog.setCancelText("Không, giữ lại");
        dialog.setCancelButtonTheme("tertiary");
        dialog.setConfirmText("Đồng ý hủy liên kết");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            try {
                memberService.unlinkUserFromMember(currentUser);
                toastService.info("Đã hủy liên kết thẻ thư viện khỏi tài khoản!");
                loadMemberData();
            } catch (Exception ex) {
                toastService.error("Lỗi khi hủy liên kết: " + ex.getMessage());
            }
        });
        dialog.open();
    }

    // =========================================================================
    // TRƯỜNG HỢP 2: CHƯA LIÊN KẾT THẺ (Tra cứu & Xác nhận thêm thẻ)
    // =========================================================================
    private void renderUnlinkedIntro() {
        unlinkedIntroBanner.removeAll();

        Div alert = new Div();
        alert.addClassNames("bg-warning-10", "border", "border-warning-30", "rounded-m", "p-m", "box-border");

        HorizontalLayout h = new HorizontalLayout();
        h.setWidthFull();
        h.setSpacing(true);
        h.setAlignItems(FlexComponent.Alignment.CENTER);

        Div iconDiv = new Div(VaadinIcon.EXCLAMATION_CIRCLE.create());
        iconDiv.getStyle().set("font-size", "28px").set("color", "var(--lumo-warning-color, #f59e0b)")
                .set("flex-shrink", "0");

        VerticalLayout v = new VerticalLayout();
        v.setPadding(false);
        v.setSpacing(false);

        H4 title = new H4("Tài khoản chưa được liên kết với Thẻ Thư Viện");
        title.addClassNames("text-warning", "font-bold");
        title.getStyle().set("margin", "0 0 4px 0");

        Span desc = new Span(
                "Nếu bạn đã đăng ký làm thẻ độc giả trực tiếp tại quầy thư viện, vui lòng sử dụng biểu mẫu tra cứu bên dưới để liên kết thẻ vào tài khoản trực tuyến này.");
        desc.addClassNames("text-secondary", "text-s");

        v.add(title, desc);
        h.add(iconDiv, v);
        alert.add(h);

        unlinkedIntroBanner.add(alert);
    }

    private void onSearchCardClick(ClickEvent<Button> event) {
        String code = searchMemberCodeField.getValue();
        String idNum = searchIdNumberField.getValue();

        if ((code == null || code.isBlank()) && (idNum == null || idNum.isBlank())) {
            toastService.warning("Vui lòng nhập Mã thẻ thư viện hoặc Số CCCD/CMND để tìm kiếm!");
            return;
        }

        searchResultWrapper.removeAll();

        Optional<Member> foundOpt = memberService.findMemberForLinking(code, idNum);
        if (foundOpt.isEmpty()) {
            Div notFound = new Div();
            notFound.addClassNames("bg-error-10", "border", "border-error-30", "rounded-m", "p-m", "box-border");
            Span msg = new Span(
                    "Không tìm thấy thẻ thư viện nào khớp với thông tin đã nhập. Vui lòng kiểm tra lại chính xác Mã thẻ (ví dụ: LIB2026-000002) hoặc Số CCCD/CMND khi đăng ký tại thư viện!");
            msg.addClassNames("text-error", "font-medium", "text-s");
            notFound.add(msg);
            searchResultWrapper.add(notFound);
            return;
        }

        Member target = foundOpt.get();

        // Kiểm tra xem thẻ đã liên kết với tài khoản nào chưa
        if (target.getUser() != null) {
            Div alreadyLinked = new Div();
            alreadyLinked.addClassNames("bg-warning-10", "border", "border-warning-30", "rounded-m", "p-m",
                    "box-border");

            String linkedUser = target.getUser().getUsername();
            boolean isMine = currentUser != null && target.getUser().getId().equals(currentUser.getId());

            Span msg = new Span(isMine
                    ? "Thẻ thư viện này đã được liên kết với chính tài khoản của bạn rồi!"
                    : String.format(
                            "Thẻ thư viện %s đã được liên kết với tài khoản '%s'. Nếu bạn là chủ sở hữu hợp pháp của thẻ này, vui lòng liên hệ thủ thư tại quầy để được hỗ trợ!",
                            target.getMemberCode(), linkedUser));
            msg.addClassNames("text-warning", "font-medium", "text-s");
            alreadyLinked.add(msg);
            searchResultWrapper.add(alreadyLinked);
            return;
        }

        // Tìm thấy thẻ hợp lệ CHƯA LIÊN KẾT -> Hiển thị dạng Card xem trước & Nút xác
        // nhận thêm thẻ
        Div previewBox = new Div();
        previewBox.addClassNames("bg-success-5", "border", "border-success-30", "rounded-m", "p-m", "box-border",
                "flex", "flex-col", "gap-m");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        Div checkIcon = new Div(VaadinIcon.CHECK_CIRCLE.create());
        checkIcon.getStyle().set("font-size", "22px").set("color", "var(--lumo-success-color, #16a34a)");

        Span headerText = new Span("Đã tìm thấy hồ sơ Thẻ thư viện hợp lệ! Vui lòng kiểm tra thông tin bên dưới:");
        headerText.addClassNames("text-success", "font-bold", "text-m");
        header.add(checkIcon, headerText);

        // Preview Card chi tiết
        Div cardPreview = new Div();
        cardPreview.addClassNames("bg-base", "rounded-m", "border", "border-contrast-10", "p-m", "shadow-xs",
                "box-border");

        Div infoGrid = new Div();
        infoGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(180px, 1fr))")
                .set("gap", "10px");

        infoGrid.add(
                createDetailItem("Mã thẻ thư viện", target.getMemberCode(), "vaadin:barcode"),
                createDetailItem("Họ và tên chủ thẻ", target.getFullName(), "vaadin:user"),
                createDetailItem("Số CCCD / CMND",
                        target.getIdNumber() != null ? target.getIdNumber() : "Chưa cập nhật", "vaadin:diploma"),
                createDetailItem("Loại độc giả",
                        target.getMemberType() != null ? target.getMemberType().getName() : "-", "vaadin:tag"),
                createDetailItem("Ngày cấp thẻ",
                        target.getCardIssuedDate() != null ? target.getCardIssuedDate().format(DATE_FMT) : "-",
                        "vaadin:calendar"),
                createDetailItem("Ngày hết hạn",
                        target.getCardExpiryDate() != null ? target.getCardExpiryDate().format(DATE_FMT) : "-",
                        "vaadin:calendar-clock"));
        cardPreview.add(infoGrid);

        // Nút Xác nhận thêm thẻ
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.setSpacing(true);

        Button confirmBtn = new Button("Xác nhận liên kết thẻ vào tài khoản", VaadinIcon.LINK.create(),
                e -> onConfirmLinkClick(target));
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button cancelBtn = new Button("Hủy bỏ", e -> searchResultWrapper.removeAll());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        actions.add(cancelBtn, confirmBtn);

        previewBox.add(header, cardPreview, actions);
        searchResultWrapper.add(previewBox);
    }

    private void onConfirmLinkClick(Member targetMember) {
        if (currentUser == null) {
            toastService.warning("Vui lòng đăng nhập để thực hiện liên kết thẻ!");
            return;
        }

        try {
            Member linked = memberService.linkUserToMember(currentUser, targetMember.getId());
            toastService.success(
                    String.format("Liên kết Thẻ thư viện %s vào tài khoản thành công!", linked.getMemberCode()));
            searchMemberCodeField.clear();
            searchIdNumberField.clear();
            searchResultWrapper.removeAll();
            loadMemberData();
        } catch (Exception ex) {
            toastService.error("Không thể liên kết thẻ: " + ex.getMessage());
        }
    }
}
