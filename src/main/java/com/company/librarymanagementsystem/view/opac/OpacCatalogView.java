package com.company.librarymanagementsystem.view.opac;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.LoanService;
import com.company.librarymanagementsystem.service.OpacService;
import com.company.librarymanagementsystem.service.ReaderLoanService;
import com.company.librarymanagementsystem.service.ReservationService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "opac", layout = MainView.class)
@ViewController("OpacCatalogView")
@ViewDescriptor("opac-catalog-view.xml")
public class OpacCatalogView extends StandardView {

    public static final String DEFAULT_COVER = "https://salt.tikicdn.com/cache/280x280/media/catalog/product/tmp/08/7d/dc/0ca9645067c022a7825058aaa16b0a1b.png";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @ViewComponent
    private Div kpiContainer;

    @ViewComponent
    private Button viewModeCardBtn;
    @ViewComponent
    private Button viewModeTableBtn;

    @ViewComponent
    private Button tabAllBtn;
    @ViewComponent
    private Button tabRecentBtn;
    @ViewComponent
    private Button tabPopularBtn;

    @ViewComponent
    private TextField searchKeywordField;
    @ViewComponent
    private EntityComboBox<Category> categoryFilter;
    @ViewComponent
    private EntityComboBox<Author> authorFilter;
    @ViewComponent
    private Checkbox onlyAvailableCheckbox;
    @ViewComponent
    private Button searchBtn;
    @ViewComponent
    private Button clearFilterBtn;

    @ViewComponent
    private VerticalLayout cardContainer;
    @ViewComponent
    private Div bookCardsGrid;
    @ViewComponent
    private VerticalLayout tableContainer;
    @ViewComponent
    private DataGrid<Book> booksDataGrid;
    @ViewComponent
    private Span resultsCountLabel;
    @ViewComponent
    private io.jmix.flowui.component.pagination.SimplePagination pagination;

    @ViewComponent
    private CollectionContainer<Book> booksDc;
    @ViewComponent
    private CollectionLoader<Book> booksDl;

    @Autowired
    private OpacService opacService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private ReaderLoanService readerLoanService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private DataManager dataManager;

    private boolean isCardView = true;
    private String currentTab = "ALL"; // ALL, RECENT, POPULAR

    @Subscribe
    public void onInit(InitEvent event) {
        initLoaderParams();
        initStatCards();
        initTableRenderers();
        initEvents();

        booksDc.addCollectionChangeListener(e -> {
            updateResultsCount();
            if (isCardView) {
                renderCardGrid();
            }
        });
    }

    private void initLoaderParams() {
        booksDl.setParameter("keyword", null);
        booksDl.setParameter("category", null);
        booksDl.setParameter("author", null);
        booksDl.setParameter("onlyAvailable", false);
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        applyFilter();
    }

    private void initStatCards() {
        if (kpiContainer == null) return;
        kpiContainer.removeAll();

        OpacService.OpacStats stats = opacService.getStats();

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        grid.add(createKpiCard(
                "Tổng Số Đầu Sách",
                String.format("%,d", stats.totalTitles()),
                "Đầu sách lưu trữ trong kho",
                "bg-base border-contrast-10",
                "text-primary"
        ));

        grid.add(createKpiCard(
                "Tổng Số Bản Sao",
                String.format("%,d", stats.totalCopies()),
                "Cuốn sách vật lý có mã vạch",
                "bg-base border-contrast-10",
                "text-primary"
        ));

        grid.add(createKpiCard(
                "Sẵn Sàng Trên Kệ",
                String.format("%,d", stats.availableCopies()),
                "Có thể mượn ngay tại quầy",
                stats.availableCopies() > 0 ? "bg-success-10 text-success border-success-30" : "bg-base border-contrast-10",
                stats.availableCopies() > 0 ? "text-success font-bold" : "text-primary"
        ));

        grid.add(createKpiCard(
                "Đang Cho Mượn",
                String.format("%,d", stats.borrowedCopies()),
                "Đang lưu hành bên ngoài",
                stats.borrowedCopies() > 0 ? "bg-warning-10 text-warning border-warning-20" : "bg-base border-contrast-10",
                stats.borrowedCopies() > 0 ? "text-warning font-bold" : "text-primary"
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

    private void initEvents() {
        viewModeCardBtn.addClickListener(e -> switchViewMode(true));
        viewModeTableBtn.addClickListener(e -> switchViewMode(false));

        tabAllBtn.addClickListener(e -> switchTab("ALL"));
        tabRecentBtn.addClickListener(e -> switchTab("RECENT"));
        tabPopularBtn.addClickListener(e -> switchTab("POPULAR"));

        searchBtn.addClickListener(e -> applyFilter());
        clearFilterBtn.addClickListener(e -> clearFilter());

        searchKeywordField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> applyFilter());
        categoryFilter.addValueChangeListener(e -> applyFilter());
        authorFilter.addValueChangeListener(e -> applyFilter());
        onlyAvailableCheckbox.addValueChangeListener(e -> applyFilter());
    }

    private void switchViewMode(boolean cardMode) {
        this.isCardView = cardMode;
        if (cardMode) {
            viewModeCardBtn.setThemeName("primary small");
            viewModeTableBtn.setThemeName("tertiary small");
            cardContainer.setVisible(true);
            tableContainer.setVisible(false);
            renderCardGrid();
        } else {
            viewModeCardBtn.setThemeName("tertiary small");
            viewModeTableBtn.setThemeName("primary small");
            cardContainer.setVisible(false);
            tableContainer.setVisible(true);
        }
    }

    private void switchTab(String tab) {
        this.currentTab = tab;
        tabAllBtn.setThemeName("tertiary");
        tabRecentBtn.setThemeName("tertiary");
        tabPopularBtn.setThemeName("tertiary");

        if ("ALL".equals(tab)) {
            tabAllBtn.setThemeName("primary");
            if (pagination != null) pagination.setVisible(true);
            applyFilter();
        } else if ("RECENT".equals(tab)) {
            tabRecentBtn.setThemeName("primary");
            if (pagination != null) pagination.setVisible(false);
            List<Book> recents = opacService.getRecentBooks(24);
            booksDc.setItems(recents);
        } else if ("POPULAR".equals(tab)) {
            tabPopularBtn.setThemeName("primary");
            if (pagination != null) pagination.setVisible(false);
            List<Book> populars = opacService.getPopularBooks(24);
            booksDc.setItems(populars);
        }
    }

    private void applyFilter() {
        if (!"ALL".equals(currentTab)) {
            currentTab = "ALL";
            tabAllBtn.setThemeName("primary");
            tabRecentBtn.setThemeName("tertiary");
            tabPopularBtn.setThemeName("tertiary");
            if (pagination != null) pagination.setVisible(true);
        }

        String kw = searchKeywordField.getValue();
        Category cat = categoryFilter.getValue();
        Author author = authorFilter.getValue();
        Boolean onlyAvail = onlyAvailableCheckbox.getValue();

        booksDl.setParameter("keyword", (kw != null && !kw.trim().isEmpty()) ? kw.trim() : null);
        booksDl.setParameter("category", cat);
        booksDl.setParameter("author", author);
        booksDl.setParameter("onlyAvailable", Boolean.TRUE.equals(onlyAvail));
        booksDl.load();
    }

    private void clearFilter() {
        searchKeywordField.clear();
        categoryFilter.clear();
        authorFilter.clear();
        onlyAvailableCheckbox.setValue(false);
        applyFilter();
    }

    private void updateResultsCount() {
        int count = booksDc.getItems().size();
        if (resultsCountLabel != null) {
            if ("ALL".equals(currentTab)) {
                resultsCountLabel.setText(String.format("Hiển thị %d đầu sách trên trang này", count));
            } else {
                resultsCountLabel.setText(String.format("Hiển thị Top %d đầu sách", count));
            }
        }
    }

    /**
     * Render lưới Card dạng Grid hiện đại (như Tiki / Goodreads)
     */
    private void renderCardGrid() {
        bookCardsGrid.removeAll();
        bookCardsGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(260px, 1fr))")
                .set("gap", "20px")
                .set("margin-top", "12px");

        List<Book> books = booksDc.getItems();
        if (books.isEmpty()) {
            Div emptyDiv = new Div();
            emptyDiv.getStyle()
                    .set("grid-column", "1 / -1")
                    .set("text-align", "center")
                    .set("padding", "40px")
                    .set("color", "#64748b")
                    .set("background", "#f8fafc")
                    .set("border-radius", "8px");
            emptyDiv.add(new H4("Không tìm thấy đầu sách nào phù hợp với điều kiện tìm kiếm!"));
            emptyDiv.add(new Span("Hãy thử tìm bằng từ khóa khác hoặc bỏ chọn 'Chỉ sách còn trên kệ'."));
            bookCardsGrid.add(emptyDiv);
            return;
        }

        for (Book book : books) {
            bookCardsGrid.add(createBookCard(book));
        }
    }

    private Div createBookCard(Book book) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "10px")
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.04)")
                .set("transition", "transform 0.15s ease, box-shadow 0.15s ease");

        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle().set("transform", "translateY(-3px)").set("box-shadow", "0 6px 12px rgba(0,0,0,0.08)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle().set("transform", "translateY(0)").set("box-shadow", "0 2px 4px rgba(0,0,0,0.04)");
        });

        // Ảnh bìa sách
        Div imgContainer = new Div();
        imgContainer.getStyle()
                .set("height", "210px")
                .set("background", "#f1f5f9")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "pointer")
                .set("overflow", "hidden");

        String coverUrl = book.getCoverImageUrl() != null && !book.getCoverImageUrl().isBlank()
                ? book.getCoverImageUrl() : DEFAULT_COVER;

        Image img = new Image(coverUrl, book.getTitle());
        img.getStyle()
                .set("max-height", "190px")
                .set("max-width", "140px")
                .set("object-fit", "cover")
                .set("border-radius", "4px")
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.15)");
        imgContainer.add(img);
        imgContainer.addClickListener(e -> openBookDetailDialog(book));

        // Nội dung card
        Div content = new Div();
        content.getStyle()
                .set("padding", "14px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("flex-grow", "1");

        // Tiêu đề sách
        Span titleSpan = new Span(book.getTitle());
        titleSpan.getStyle()
                .set("font-weight", "700")
                .set("font-size", "15px")
                .set("color", "#1e293b")
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "2")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("min-height", "42px")
                .set("cursor", "pointer");
        titleSpan.addClickListener(e -> openBookDetailDialog(book));

        // Tác giả có nhãn rõ ràng
        String authors = (book.getAuthors() != null && !book.getAuthors().isEmpty())
                ? book.getAuthors().stream().map(Author::getFullName).collect(Collectors.joining(", "))
                : "Chưa rõ tác giả";
        Div authorDiv = new Div();
        authorDiv.getStyle()
                .set("font-size", "13px")
                .set("color", "#475569")
                .set("margin-top", "6px")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        Span authorLabel = new Span("Tác giả: ");
        authorLabel.getStyle().set("font-weight", "600").set("color", "#64748b");
        Span authorVal = new Span(authors);
        authorVal.getStyle().set("font-style", "italic").set("color", "#1e293b");
        authorDiv.add(authorLabel, authorVal);

        // Thể loại có nhãn rõ ràng
        String category = (book.getCategories() != null && !book.getCategories().isEmpty())
                ? book.getCategories().get(0).getName() : "Chung";
        Div catDiv = new Div();
        catDiv.getStyle()
                .set("font-size", "12px")
                .set("margin-top", "5px")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        Span catLabel = new Span("Thể loại: ");
        catLabel.getStyle().set("font-weight", "600").set("color", "#64748b");
        Span catBadge = new Span(category);
        catBadge.getStyle()
                .set("background", "#eff6ff")
                .set("color", "#2563eb")
                .set("padding", "2px 8px")
                .set("border-radius", "6px")
                .set("font-size", "11px")
                .set("font-weight", "600");
        catDiv.add(catLabel, catBadge);

        // Mã ISBN & Năm xuất bản
        String isbnStr = (book.getIsbn() != null && !book.getIsbn().isBlank()) ? book.getIsbn() : "-";
        String yearStr = (book.getPublishYear() != null && !book.getPublishYear().isBlank()) ? "Năm: " + book.getPublishYear() : "";
        Div isbnRow = new Div();
        isbnRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("font-size", "11px")
                .set("margin-top", "5px")
                .set("color", "#64748b");
        Div isbnDiv = new Div();
        Span isbnLabel = new Span("ISBN: ");
        isbnLabel.getStyle().set("font-weight", "600").set("color", "#64748b");
        Span isbnVal = new Span(isbnStr);
        isbnVal.getStyle().set("font-family", "monospace").set("color", "#334155");
        isbnDiv.add(isbnLabel, isbnVal);
        Span yearSpan = new Span(yearStr);
        yearSpan.getStyle().set("color", "#94a3b8");
        isbnRow.add(isbnDiv, yearSpan);

        // Tình trạng bản sao trên kệ (dùng dữ liệu nạp sẵn trong fetchPlan, không query lại DB)
        OpacService.BookAvailability avail = opacService.getBookAvailability(book);

        Div availBadge = new Div();
        availBadge.getStyle()
                .set("margin-top", "10px")
                .set("padding", "6px 10px")
                .set("border-radius", "6px")
                .set("font-size", "12px")
                .set("font-weight", "600");

        Span shelfSpan = new Span();
        shelfSpan.getStyle()
                .set("font-size", "11px")
                .set("color", "#475569")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("margin-top", "4px");

        if (avail.totalCopies() == 0) {
            availBadge.setText("Chưa có bản sao");
            availBadge.getStyle().set("background", "#f1f5f9").set("color", "#64748b").set("border", "1px solid #e2e8f0");
            shelfSpan.setText("Chưa nhập kho bản sao");
        } else if (avail.availableCopies() > 0) {
            availBadge.setText(String.format("Còn %d cuốn trên kệ", avail.availableCopies()));
            availBadge.getStyle().set("background", "#dcfce7").set("color", "#15803d");
            shelfSpan.setText("Vị trí: " + avail.locationSummary());
        } else {
            availBadge.setText("Đang mượn hết");
            availBadge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
            shelfSpan.setText("Kệ: " + avail.locationSummary());
        }

        // Hàng nút bấm
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "12px");

        Button detailBtn = new Button("Chi tiết & Kệ", e -> openBookDetailDialog(book));
        detailBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        detailBtn.setWidthFull();
        actions.add(detailBtn);

        if (avail.canReserve()) {
            Button reserveBtn = new Button("Đặt trước", e -> onReserveBookClick(book));
            reserveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            actions.add(reserveBtn);
        }

        content.add(titleSpan, authorDiv, catDiv, isbnRow, availBadge, shelfSpan, actions);
        card.add(imgContainer, content);
        return card;
    }

    private void initTableRenderers() {
        // Ảnh bìa nhỏ
        booksDataGrid.getColumnByKey("coverCol").setRenderer(new ComponentRenderer<>(b -> {
            String url = b.getCoverImageUrl() != null && !b.getCoverImageUrl().isBlank()
                    ? b.getCoverImageUrl() : DEFAULT_COVER;
            Image img = new Image(url, b.getTitle());
            img.setWidth("38px");
            img.setHeight("52px");
            img.getStyle().set("object-fit", "cover").set("border-radius", "4px").set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)");
            return img;
        }));

        // Tác giả
        booksDataGrid.getColumnByKey("authorsCol").setRenderer(new ComponentRenderer<>(b -> {
            String authors = (b.getAuthors() != null && !b.getAuthors().isEmpty())
                    ? b.getAuthors().stream().map(Author::getFullName).collect(Collectors.joining(", ")) : "-";
            return new Span(authors);
        }));

        // Thể loại
        booksDataGrid.getColumnByKey("categoriesCol").setRenderer(new ComponentRenderer<>(b -> {
            String cats = (b.getCategories() != null && !b.getCategories().isEmpty())
                    ? b.getCategories().stream().map(Category::getName).collect(Collectors.joining(", ")) : "-";
            return new Span(cats);
        }));

        // Tình trạng trên kệ
        booksDataGrid.getColumnByKey("availabilityCol").setRenderer(new ComponentRenderer<>(b -> {
            OpacService.BookAvailability avail = opacService.getBookAvailability(b);
            Span badge = new Span();
            badge.getStyle().set("padding", "3px 8px").set("border-radius", "6px").set("font-size", "12px").set("font-weight", "600");
            if (avail.totalCopies() == 0) {
                badge.setText("Chưa có bản sao");
                badge.getStyle().set("background", "#f1f5f9").set("color", "#64748b").set("border", "1px solid #e2e8f0");
            } else if (avail.availableCopies() > 0) {
                badge.setText(String.format("Còn %d cuốn", avail.availableCopies()));
                badge.getStyle().set("background", "#dcfce7").set("color", "#15803d");
            } else {
                badge.setText("Đang mượn hết");
                badge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
            }
            return badge;
        }));

        // Thao tác
        booksDataGrid.getColumnByKey("actionCol").setRenderer(new ComponentRenderer<>(b -> {
            HorizontalLayout cellActions = new HorizontalLayout();
            cellActions.setSpacing(true);
            cellActions.setAlignItems(FlexComponent.Alignment.CENTER);

            OpacService.BookAvailability avail = opacService.getBookAvailability(b.getId());
            if (avail.canReserve()) {
                Button reserve = new Button("Đặt trước", e -> onReserveBookClick(b));
                reserve.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
                cellActions.add(reserve);
            }

            Button detail = new Button("Xem kệ", e -> openBookDetailDialog(b));
            detail.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            cellActions.add(detail);

            return cellActions;
        }));
    }

    /**
     * Dialog Chi Tiết Sách & Tình Trạng Bản Sao Vật Lý Trên Kệ (G02)
     */
    private void openBookDetailDialog(Book book) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Thông Tin Sách & Vị Trí Kệ Thư Viện");
        dialog.setWidth("54em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        OpacService.BookAvailability avail = opacService.getBookAvailability(book.getId());

        // Hàng thông tin chính
        HorizontalLayout mainInfo = new HorizontalLayout();
        mainInfo.setWidthFull();
        mainInfo.setSpacing(true);
        mainInfo.setAlignItems(FlexComponent.Alignment.START);

        // Bìa sách to
        String coverUrl = book.getCoverImageUrl() != null && !book.getCoverImageUrl().isBlank()
                ? book.getCoverImageUrl() : DEFAULT_COVER;
        Image largeCover = new Image(coverUrl, book.getTitle());
        largeCover.setWidth("150px");
        largeCover.setHeight("215px");
        largeCover.getStyle()
                .set("object-fit", "cover")
                .set("border-radius", "6px")
                .set("box-shadow", "0 4px 8px rgba(0,0,0,0.15)")
                .set("border", "1px solid #e2e8f0");

        // Chi tiết
        VerticalLayout textInfo = new VerticalLayout();
        textInfo.setSpacing(false);
        textInfo.setPadding(false);

        H3 title = new H3(book.getTitle());
        title.getStyle().set("margin", "0 0 4px 0").set("color", "#1e293b");

        String authors = (book.getAuthors() != null && !book.getAuthors().isEmpty())
                ? book.getAuthors().stream().map(Author::getFullName).collect(Collectors.joining(", ")) : "Chưa rõ";
        Span authorSpan = new Span("Tác giả: " + authors);
        authorSpan.getStyle().set("font-weight", "600").set("color", "#2563eb").set("margin-bottom", "6px");

        String categories = (book.getCategories() != null && !book.getCategories().isEmpty())
                ? book.getCategories().stream().map(Category::getName).collect(Collectors.joining(", ")) : "Chưa phân loại";
        String pub = book.getPublisher() != null ? book.getPublisher().getName() : "NXB chưa rõ";
        String year = book.getPublishYear() != null ? book.getPublishYear() : "-";
        String isbn = book.getIsbn() != null ? book.getIsbn() : "-";
        String ddc = book.getDdc() != null ? book.getDdc() : "-";

        Div metaDiv = new Div();
        metaDiv.getStyle().set("font-size", "13px").set("color", "#475569").set("line-height", "1.6");
        metaDiv.add(new Div(new Span("• Thể loại: " + categories)));
        metaDiv.add(new Div(new Span(String.format("• Nhà xuất bản: %s (%s)", pub, year))));
        metaDiv.add(new Div(new Span(String.format("• Mã ISBN: %s | Phân loại DDC: %s", isbn, ddc))));

        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            Paragraph descP = new Paragraph(book.getDescription());
            descP.getStyle()
                    .set("font-size", "12px")
                    .set("color", "#64748b")
                    .set("margin-top", "8px")
                    .set("display", "-webkit-box")
                    .set("-webkit-line-clamp", "3")
                    .set("-webkit-box-orient", "vertical")
                    .set("overflow", "hidden");
            metaDiv.add(descP);
        }

        textInfo.add(title, authorSpan, metaDiv);
        mainInfo.add(largeCover, textInfo);

        // Thống kê nhanh tình trạng bản sao
        HorizontalLayout statRow = new HorizontalLayout();
        statRow.setWidthFull();
        statRow.setSpacing(true);

        statRow.add(
                createMiniStat("Tổng bản sao", avail.totalCopies() + " cuốn", "#0f172a", "#f8fafc"),
                createMiniStat("Còn trên kệ", avail.availableCopies() + " cuốn", "#15803d", "#f0fdf4"),
                createMiniStat("Đang cho mượn", avail.borrowedCopies() + " cuốn", "#b45309", "#fffbeb"),
                createMiniStat("Đang chờ đặt trước", avail.pendingReservations() + " người", "#6b21a8", "#faf5ff")
        );

        // Bảng danh sách các cuốn sách vật lý trên kệ
        H4 copiesTitle = new H4("Danh sách các bản sao vật lý & vị trí kệ:");
        copiesTitle.getStyle().set("margin", "8px 0 4px 0").set("color", "#334155");

        List<OpacService.CopyInfo> copyList = opacService.getCopiesForBook(book.getId());
        Grid<OpacService.CopyInfo> copiesGrid = new Grid<>();
        copiesGrid.setWidthFull();
        copiesGrid.setAllRowsVisible(true);
        copiesGrid.setItems(copyList);

        copiesGrid.addColumn(OpacService.CopyInfo::barcode).setHeader("Mã vạch (Barcode)").setAutoWidth(true);
        copiesGrid.addColumn(OpacService.CopyInfo::locationName).setHeader("Vị trí kệ sách").setAutoWidth(true);
        copiesGrid.addColumn(OpacService.CopyInfo::condition).setHeader("Tình trạng").setAutoWidth(true);

        copiesGrid.addColumn(new ComponentRenderer<>(c -> {
            Span badge = new Span();
            badge.getStyle().set("padding", "2px 8px").set("border-radius", "4px").set("font-size", "11px").set("font-weight", "600");
            if (BookCopyStatus.AVAILABLE.getId().equalsIgnoreCase(c.status())) {
                badge.setText("SẴN SÀNG TRÊN KỆ");
                badge.getStyle().set("background", "#dcfce7").set("color", "#15803d");
            } else if (BookCopyStatus.BORROWED.getId().equalsIgnoreCase(c.status())) {
                String due = c.dueDate() != null ? " (Hạn: " + c.dueDate().format(DATE_FMT) + ")" : "";
                badge.setText("ĐANG MƯỢN" + due);
                badge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
            } else if (BookCopyStatus.RESERVED.getId().equalsIgnoreCase(c.status())) {
                badge.setText("GIỮ CHỖ ĐẶT TRƯỚC");
                badge.getStyle().set("background", "#f3e8ff").set("color", "#7e22ce");
            } else {
                badge.setText(c.status());
            }
            return badge;
        })).setHeader("Trạng thái").setAutoWidth(true);

        layout.add(mainInfo, statRow, copiesTitle, copiesGrid);

        // Cảnh báo / Hướng dẫn
        if (avail.totalCopies() == 0) {
            Div emptyBox = new Div();
            emptyBox.getStyle()
                    .set("background", "#f8fafc")
                    .set("border", "1px solid #cbd5e1")
                    .set("border-radius", "6px")
                    .set("padding", "10px 14px")
                    .set("font-size", "13px")
                    .set("color", "#64748b")
                    .set("width", "100%");
            emptyBox.setText("Đầu sách này hiện chưa có bản sao vật lý nào trong kho thư viện. Vui lòng quay lại sau hoặc liên hệ thủ thư!");
            layout.add(emptyBox);
        } else if (avail.availableCopies() > 0) {
            Div guideBox = new Div();
            guideBox.getStyle()
                    .set("background", "#f0fdf4")
                    .set("border", "1px solid #86efac")
                    .set("border-radius", "6px")
                    .set("padding", "10px 14px")
                    .set("font-size", "13px")
                    .set("color", "#166534")
                    .set("width", "100%");
            guideBox.setText(String.format("Sách đang có sẵn trên kệ. Mời bạn đến vị trí: %s để lấy sách và đến quầy làm thủ tục mượn!",
                    avail.locationSummary()));
            layout.add(guideBox);
        } else {
            Div warnBox = new Div();
            warnBox.getStyle()
                    .set("background", "#fef2f2")
                    .set("border", "1px solid #fca5a5")
                    .set("border-radius", "6px")
                    .set("padding", "10px 14px")
                    .set("font-size", "13px")
                    .set("color", "#991b1b")
                    .set("width", "100%");
            warnBox.setText(String.format("Toàn bộ %d bản sao của cuốn sách này hiện đã được mượn hết. Bạn có thể bấm 'Đặt trước' để xếp hàng nhận sách khi có bản sao trả về!",
                    avail.totalCopies()));
            layout.add(warnBox);
        }

        dialog.add(layout);

        Button closeBtn = new Button("Đóng", e -> dialog.close());
        dialog.getFooter().add(closeBtn);

        if (avail.canReserve()) {
            Button reserveBtn = new Button("Đặt trước cuốn sách này", e -> {
                dialog.close();
                onReserveBookClick(book);
            });
            reserveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
            dialog.getFooter().add(reserveBtn);
        }

        dialog.open();
    }

    private Div createMiniStat(String label, String val, String color, String bg) {
        Div div = new Div();
        div.getStyle()
                .set("background", bg)
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "6px")
                .set("padding", "8px 12px")
                .set("text-align", "center")
                .set("width", "100%");

        Span l = new Span(label);
        l.getStyle().set("font-size", "11px").set("color", "#64748b").set("display", "block");

        Span v = new Span(val);
        v.getStyle().set("font-size", "15px").set("font-weight", "700").set("color", color);

        div.add(l, v);
        return div;
    }

    /**
     * Xử lý Đặt trước sách cho độc giả đang đăng nhập
     */
    private void onReserveBookClick(Book book) {
        User currentUser = null;
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User u) {
            currentUser = u;
        }

        if (currentUser == null) {
            toastService.warning("Vui lòng đăng nhập để thực hiện đặt trước sách!");
            return;
        }

        // Tìm Member tương ứng với User đang đăng nhập
        Optional<Member> memberOpt = dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.user = :u")
                .parameter("u", currentUser)
                .optional();

        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            ReservationService.ReservationValidationResult val = reservationService.validateReservation(member, book);
            if (!val.isValid()) {
                toastService.warning(val.errorMessage());
                return;
            }

            try {
                Reservation res = reservationService.createReservation(member, book);
                Long queuePos = dataManager.loadValue(
                        "select count(r) from Reservation r where r.book = :b and r.status = 'PENDING' and r.reservedAt <= :at", Long.class)
                        .parameter("b", book)
                        .parameter("at", res.getReservedAt())
                        .one();
                long qPos = queuePos != null ? queuePos : 1L;

                toastService.success(String.format("Đặt trước sách '%s' thành công! Thứ tự hàng đợi của bạn: #%d",
                        book.getTitle(), qPos));
                booksDl.load();
                initStatCards();
            } catch (Exception ex) {
                toastService.error("Lỗi khi đặt trước: " + ex.getMessage());
            }
        } else {
            // Trường hợp tài khoản Thủ thư hoặc Quản trị viên đang xem cổng OPAC
            openLibrarianReserveDialog(book, currentUser);
        }
    }

    /**
     * Nếu là Thủ thư/Admin test OPAC, cho phép chọn độc giả để đặt trước
     */
    private void openLibrarianReserveDialog(Book book, User currentUser) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Đặt Trước Sách (Thủ thư thực hiện thay)");
        dialog.setWidth("34em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        Span info = new Span(String.format("Tài khoản của bạn (%s) là Quản trị/Thủ thư. Vui lòng chọn độc giả để lập lượt đặt trước cho sách: '%s'",
                currentUser.getUsername(), book.getTitle()));
        info.getStyle().set("font-size", "13px").set("color", "#475569");

        ComboBox<Member> memberCombo = new ComboBox<>("Chọn độc giả đặt trước");
        memberCombo.setWidthFull();
        memberCombo.setItems(dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.status = 'ACTIVE' order by m.fullName asc")
                .list());
        memberCombo.setItemLabelGenerator(m -> String.format("[%s] %s", m.getMemberCode(), m.getFullName()));

        layout.add(info, memberCombo);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận Đặt trước", e -> {
            Member m = memberCombo.getValue();
            if (m == null) {
                toastService.warning("Vui lòng chọn độc giả!");
                return;
            }

            ReservationService.ReservationValidationResult val = reservationService.validateReservation(m, book);
            if (!val.isValid()) {
                toastService.warning(val.errorMessage());
                return;
            }

            try {
                Reservation res = reservationService.createReservation(m, book);
                Long queuePos = dataManager.loadValue(
                        "select count(r) from Reservation r where r.book = :b and r.status = 'PENDING' and r.reservedAt <= :at", Long.class)
                        .parameter("b", book)
                        .parameter("at", res.getReservedAt())
                        .one();
                long qPos = queuePos != null ? queuePos : 1L;

                toastService.success(String.format("Đã đặt trước thành công cho độc giả %s! Vị trí hàng đợi: #%d",
                        m.getFullName(), qPos));
                dialog.close();
                booksDl.load();
                initStatCards();
            } catch (Exception ex) {
                toastService.error("Lỗi khi đặt trước: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Hủy", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }
}
