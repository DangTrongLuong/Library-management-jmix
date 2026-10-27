package com.company.librarymanagementsystem.view.book;

import com.company.librarymanagementsystem.entity.Author;
import com.company.librarymanagementsystem.entity.Book;
import com.company.librarymanagementsystem.entity.Category;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.valuepicker.JmixValuePicker;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "books/:id", layout = MainView.class)
@ViewController(id = "Book.detail")
@ViewDescriptor(path = "book-detail-view.xml")
@EditedEntityContainer("bookDc")
@DialogMode(width = "54em")
public class BookDetailView extends StandardDetailView<Book> {

    private static final Logger log = LoggerFactory.getLogger(BookDetailView.class);
    public static final String DEFAULT_COVER_IMAGE = "https://thuvien.vov.gov.vn/assets/img/default.jpg";

    @ViewComponent
    private TextField titleField;

    @ViewComponent
    private TextField coverImageUrlField;

    @ViewComponent
    private Image coverImagePreview;

    @ViewComponent
    private Span coverStatusText;

    @ViewComponent
    private Select<String> statusField;

    @ViewComponent
    private Select<String> languageField;

    @ViewComponent
    private JmixValuePicker<String> authorsPicker;

    @ViewComponent
    private JmixValuePicker<String> categoriesPicker;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private EntityStates entityStates;

    @Autowired
    private ToastService toastService;

    private List<Author> selectedAuthors = new ArrayList<>();
    private List<Category> selectedCategories = new ArrayList<>();
    private boolean newEntity;

    @Subscribe
    public void onInit(final InitEvent event) {
        setShowSaveNotification(false);

        statusField.setItems(List.of("ACTIVE", "HIDDEN"));
        statusField.setItemLabelGenerator(status -> switch (status) {
            case "ACTIVE" -> "Đang lưu hành";
            case "HIDDEN" -> "Tạm ẩn";
            default -> status != null ? status : "";
        });

        languageField.setItems(List.of("vi", "en", "ja", "zh", "fr", "ko", "ru", "de", "other"));
        languageField.setItemLabelGenerator(lang -> switch (lang) {
            case "vi" -> "Tiếng Việt";
            case "en" -> "Tiếng Anh";
            case "ja" -> "Tiếng Nhật";
            case "zh" -> "Tiếng Trung";
            case "fr" -> "Tiếng Pháp";
            case "ko" -> "Tiếng Hàn";
            case "ru" -> "Tiếng Nga";
            case "de" -> "Tiếng Đức";
            case "other" -> "Khác";
            default -> lang != null ? lang : "";
        });
    }

    @Subscribe
    public void onInitEntity(final InitEntityEvent<Book> event) {
        Book book = event.getEntity();
        book.setTitle(null);
        book.setSubtitle(null);
        book.setIsbn(null);
        book.setDdc(null);
        book.setPublisher(null);
        book.setAuthors(new ArrayList<>());
        book.setCategories(new ArrayList<>());
        book.setPublishYear(null);
        book.setEdition(null);
        book.setPageCount(null);
        book.setPrice(null);
        book.setDescription(null);
        book.setCoverImageUrl(null);
        book.setLanguage("vi");
        book.setStatus("ACTIVE");
        book.setIsReference(false);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        titleField.setInvalid(false);
        titleField.setErrorMessage(null);

        if (getEditedEntity().getAuthors() != null) {
            this.selectedAuthors = new ArrayList<>(getEditedEntity().getAuthors());
        } else {
            this.selectedAuthors = new ArrayList<>();
        }
        updateAuthorsPicker();

        if (getEditedEntity().getCategories() != null) {
            this.selectedCategories = new ArrayList<>(getEditedEntity().getCategories());
        } else {
            this.selectedCategories = new ArrayList<>();
        }
        updateCategoriesPicker();

        if (getEditedEntity().getLanguage() == null) {
            getEditedEntity().setLanguage("vi");
            languageField.setValue("vi");
        }

        String currentCover = getEditedEntity().getCoverImageUrl();
        if (currentCover != null && !currentCover.trim().isEmpty()) {
            updateCoverPreview(currentCover);
            coverStatusText.setText("");
        } else {
            updateCoverPreview(DEFAULT_COVER_IMAGE);
            coverStatusText.setText("Chưa có ảnh bìa");
            coverStatusText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        }
    }

    @Subscribe("titleField")
    public void onTitleFieldBlur(final BlurNotifier.BlurEvent<TextField> event) {
        autoFetchCoverFromTiki();
    }

    @Subscribe("fetchTikiBtn")
    public void onFetchTikiBtnClick(final ClickEvent<Button> event) {
        autoFetchCoverFromTiki();
    }

    @Subscribe("coverImageUrlField")
    public void onCoverImageUrlFieldValueChange(final ComponentValueChangeEvent<TextField, String> event) {
        updateCoverPreview(event.getValue());
    }

    public void autoFetchCoverFromTiki() {
        String title = titleField.getValue();
        if (title == null || title.trim().isEmpty()) {
            return;
        }

        coverStatusText.setText("Đang tìm ảnh từ Tiki...");
        coverStatusText.getStyle().set("color", "var(--lumo-primary-text-color)");

        String foundUrl = fetchTikiCoverImage(title);
        if (foundUrl != null && !foundUrl.isBlank()) {
            coverImageUrlField.setValue(foundUrl);
            getEditedEntity().setCoverImageUrl(foundUrl);
            updateCoverPreview(foundUrl);
            coverStatusText.setText("Đã tìm thấy ảnh từ Tiki");
            coverStatusText.getStyle().set("color", "var(--lumo-success-text-color)");
            toastService.info("Đã tự động lấy ảnh bìa từ Tiki!");
        } else {
            coverStatusText.setText("Không tìm thấy ảnh của cuốn sách này");
            coverStatusText.getStyle().set("color", "var(--lumo-error-text-color)");
            toastService.warning("Không tìm thấy ảnh của cuốn sách này");
            String current = coverImageUrlField.getValue();
            if (current == null || current.trim().isEmpty()) {
                updateCoverPreview(DEFAULT_COVER_IMAGE);
            }
        }
    }

    private void updateCoverPreview(String url) {
        if (coverImagePreview != null) {
            coverImagePreview.getStyle().set("object-fit", "cover");
            if (url != null && !url.trim().isEmpty()) {
                coverImagePreview.setSrc(url.trim());
            } else {
                coverImagePreview.setSrc(DEFAULT_COVER_IMAGE);
            }
        }
    }

    private String fetchTikiCoverImage(String title) {
        try {
            String cleanTitle = title.trim();
            if (cleanTitle.contains(" - ")) {
                cleanTitle = cleanTitle.split(" - ")[0].trim();
            }
            String encoded = URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8);
            String apiUrl = "https://tiki.vn/api/v2/products?q=" + encoded + "&limit=3";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response.body());
                com.fasterxml.jackson.databind.JsonNode dataArray = root.path("data");
                if (dataArray.isArray() && !dataArray.isEmpty()) {
                    com.fasterxml.jackson.databind.JsonNode firstItem = dataArray.get(0);
                    String thumb = firstItem.path("thumbnail_url").asText(null);
                    if (thumb != null && !thumb.isBlank()) {
                        return thumb;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải ảnh từ Tiki cho sách '{}': {}", title, e.getMessage());
        }
        return null;
    }

    @Subscribe("authorsPicker.authorsLookupAction")
    public void onAuthorsLookupAction(final ActionPerformedEvent event) {
        dialogWindows.lookup(this, Author.class)
                .withSelectHandler(selected -> {
                    this.selectedAuthors = new ArrayList<>(selected);
                    updateAuthorsPicker();
                    toastService.info("Đã chọn " + selected.size() + " tác giả");
                })
                .open();
    }

    @Subscribe("authorsPicker.authorsClearAction")
    public void onAuthorsClearAction(final ActionPerformedEvent event) {
        this.selectedAuthors.clear();
        updateAuthorsPicker();
    }

    @Subscribe("categoriesPicker.categoriesLookupAction")
    public void onCategoriesLookupAction(final ActionPerformedEvent event) {
        dialogWindows.lookup(this, Category.class)
                .withSelectHandler(selected -> {
                    this.selectedCategories = new ArrayList<>(selected);
                    updateCategoriesPicker();
                    toastService.info("Đã chọn " + selected.size() + " thể loại");
                })
                .open();
    }

    @Subscribe("categoriesPicker.categoriesClearAction")
    public void onCategoriesClearAction(final ActionPerformedEvent event) {
        this.selectedCategories.clear();
        updateCategoriesPicker();
    }

    private void updateAuthorsPicker() {
        if (selectedAuthors != null && !selectedAuthors.isEmpty()) {
            authorsPicker.setValue(selectedAuthors.stream()
                    .map(Author::getFullName)
                    .collect(Collectors.joining(", ")));
        } else {
            authorsPicker.setValue(null);
        }
    }
    
    private void updateCategoriesPicker() {
        if (selectedCategories != null && !selectedCategories.isEmpty()) {
            categoriesPicker.setValue(selectedCategories.stream()
                    .map(Category::getName)
                    .collect(Collectors.joining(", ")));
        } else {
            categoriesPicker.setValue(null);
        }
    }

    @Autowired
    private io.jmix.core.DataManager dataManager;

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        Book book = getEditedEntity();

        // 1. Kiểm tra nhan đề sách
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            event.getErrors().add("Nhan đề sách không được để trống!");
        }

        // 2. Kiểm tra ISBN
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            event.getErrors().add("Mã ISBN không được để trống!");
        } else {
            String cleanIsbn = book.getIsbn().replaceAll("[-\\s]", "");
            if (!cleanIsbn.matches("^(?:\\d{9}[\\dXx]|\\d{13})$")) {
                event.getErrors()
                        .add("Mã ISBN không đúng định dạng (phải gồm 10 hoặc 13 chữ số, ví dụ: 978-604-1-12345-6)!");
            } else {
                List<String> existingIsbns;
                if (book.getId() == null) {
                    existingIsbns = dataManager.loadValue(
                            "select b.isbn from Book b where b.isbn is not null", String.class)
                            .list();
                } else {
                    existingIsbns = dataManager.loadValue(
                            "select b.isbn from Book b where b.isbn is not null and b.id <> :id", String.class)
                            .parameter("id", book.getId())
                            .list();
                }
                boolean isDuplicate = existingIsbns.stream()
                        .anyMatch(s -> s != null && s.replaceAll("[-\\s]", "").equalsIgnoreCase(cleanIsbn));
                if (isDuplicate) {
                    event.getErrors().add(String.format("Mã ISBN '%s' đã tồn tại cho một đầu sách khác trong hệ thống!",
                            book.getIsbn()));
                }
            }
        }

        // 3. Kiểm tra Nhà xuất bản
        if (book.getPublisher() == null) {
            event.getErrors().add("Vui lòng chọn Nhà xuất bản cho đầu sách!");
        }

        // 4. Kiểm tra Tác giả
        if (selectedAuthors == null || selectedAuthors.isEmpty()) {
            event.getErrors().add("Vui lòng chọn ít nhất một Tác giả cho đầu sách!");
        }

        // 5. Kiểm tra Thể loại
        if (selectedCategories == null || selectedCategories.isEmpty()) {
            event.getErrors().add("Vui lòng chọn ít nhất một Thể loại cho đầu sách!");
        }

        // 6. Kiểm tra Giá bìa
        if (book.getPrice() != null && book.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            event.getErrors().add("Giá bìa không được là số âm!");
        }

        // 7. Kiểm tra Số trang
        if (book.getPageCount() != null && book.getPageCount() <= 0) {
            event.getErrors().add("Số trang sách phải lớn hơn 0!");
        }

        // 8. Kiểm tra Năm xuất bản
        if (book.getPublishYear() != null && !book.getPublishYear().trim().isEmpty()) {
            String yearStr = book.getPublishYear().trim();
            if (yearStr.matches("^\\d{4}$")) {
                int y = Integer.parseInt(yearStr);
                int currentYear = java.time.Year.now().getValue();
                if (y < 1000 || y > currentYear + 1) {
                    event.getErrors().add(
                            String.format("Năm xuất bản (%d) không hợp lý (từ năm 1000 đến %d)!", y, currentYear + 1));
                }
            }
        }

        // 9. Đảm bảo có ảnh bìa mặc định khi thêm mới nếu chưa có ảnh
        if (book.getCoverImageUrl() == null || book.getCoverImageUrl().trim().isEmpty()) {
            book.setCoverImageUrl(DEFAULT_COVER_IMAGE);
            coverImageUrlField.setValue(DEFAULT_COVER_IMAGE);
            updateCoverPreview(DEFAULT_COVER_IMAGE);
        }
    }

    @ViewComponent
    private io.jmix.flowui.model.CollectionContainer<com.company.librarymanagementsystem.entity.BookCopy> copiesDc;

    @ViewComponent
    private io.jmix.flowui.model.CollectionLoader<com.company.librarymanagementsystem.entity.BookCopy> copiesDl;

    @ViewComponent
    private com.vaadin.flow.component.details.Details copiesDetails;

    @ViewComponent
    private io.jmix.flowui.component.grid.DataGrid<com.company.librarymanagementsystem.entity.BookCopy> copiesDataGrid;

    @Subscribe("addBulkCopiesBtn")
    public void onAddBulkCopiesBtnClick(
            final com.vaadin.flow.component.ClickEvent<com.vaadin.flow.component.button.Button> event) {
        if (entityStates.isNew(getEditedEntity())) {
            toastService.warning("Vui lòng lưu thông tin đầu sách trước khi nhập bản sao!");
            return;
        }

        io.jmix.flowui.view.DialogWindow<com.company.librarymanagementsystem.view.bookcopy.BulkCopyCreateView> window = dialogWindows
                .view(this, com.company.librarymanagementsystem.view.bookcopy.BulkCopyCreateView.class)
                .build();
        window.getView().setInitialBook(getEditedEntity());
        window.addAfterCloseListener(closeEvent -> {
            if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                copiesDl.load();
            }
        });
        window.open();
    }

    @Subscribe("addSingleCopyBtn")
    public void onAddSingleCopyBtnClick(
            final com.vaadin.flow.component.ClickEvent<com.vaadin.flow.component.button.Button> event) {
        if (entityStates.isNew(getEditedEntity())) {
            toastService.warning("Vui lòng lưu thông tin đầu sách trước khi thêm bản sao!");
            return;
        }

        dialogWindows.detail(copiesDataGrid)
                .withViewClass(com.company.librarymanagementsystem.view.bookcopy.BookCopyDetailView.class)
                .newEntity()
                .withInitializer(copy -> copy.setBook(getEditedEntity()))
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        copiesDl.load();
                    }
                })
                .open();
    }

    @Subscribe("printCopiesBtn")
    public void onPrintCopiesBtnClick(
            final com.vaadin.flow.component.ClickEvent<com.vaadin.flow.component.button.Button> event) {
        List<com.company.librarymanagementsystem.entity.BookCopy> items = copiesDc.getItems();
        if (items == null || items.isEmpty()) {
            toastService.warning("Chưa có bản sao nào để in mã vạch!");
            return;
        }

        io.jmix.flowui.view.DialogWindow<com.company.librarymanagementsystem.view.bookcopy.BarcodePrintView> window = dialogWindows
                .view(this, com.company.librarymanagementsystem.view.bookcopy.BarcodePrintView.class)
                .build();
        window.getView().setCopiesToPrint(new ArrayList<>(items));
        window.open();
    }

    @Supply(to = "copiesDataGrid.barcode", subject = "renderer")
    public com.vaadin.flow.data.renderer.Renderer<com.company.librarymanagementsystem.entity.BookCopy> copyBarcodeRenderer() {
        return new com.vaadin.flow.data.renderer.ComponentRenderer<>(copy -> {
            String b = (copy != null && copy.getBarcode() != null) ? copy.getBarcode() : "-";
            com.vaadin.flow.component.html.Span span = new com.vaadin.flow.component.html.Span(b);
            span.getElement().getThemeList().add("badge");
            span.getStyle().set("font-family", "monospace").set("font-weight", "bold");
            return span;
        });
    }

    @Supply(to = "copiesDataGrid.status", subject = "renderer")
    public com.vaadin.flow.data.renderer.Renderer<com.company.librarymanagementsystem.entity.BookCopy> copyStatusRenderer() {
        return new com.vaadin.flow.data.renderer.ComponentRenderer<>(copy -> {
            if (copy == null) {
                return new com.vaadin.flow.component.html.Span("-");
            }
            com.company.librarymanagementsystem.entity.BookCopyStatus status = copy.getStatusEnum();
            String label = (status != null) ? status.getCaption() : (copy.getStatus() != null ? copy.getStatus() : "-");
            com.vaadin.flow.component.html.Span span = new com.vaadin.flow.component.html.Span(
                    label != null ? label : "-");
            span.getElement().getThemeList().add("badge");
            if (status != null) {
                switch (status) {
                    case AVAILABLE -> span.getElement().getThemeList().add("success");
                    case BORROWED -> span.getElement().getThemeList().add("primary");
                    case RESERVED -> span.getElement().getThemeList().add("warning");
                    case MAINTENANCE -> span.getElement().getThemeList().add("contrast");
                    case LOST, DAMAGED -> span.getElement().getThemeList().add("error");
                    case WITHDRAWN -> span.getElement().getThemeList().add("contrast small");
                }
            }
            return span;
        });
    }

    @Supply(to = "copiesDataGrid.condition", subject = "renderer")
    public com.vaadin.flow.data.renderer.Renderer<com.company.librarymanagementsystem.entity.BookCopy> copyConditionRenderer() {
        return new com.vaadin.flow.data.renderer.ComponentRenderer<>(copy -> {
            if (copy == null) {
                return new com.vaadin.flow.component.html.Span("-");
            }
            com.company.librarymanagementsystem.entity.BookCopyCondition cond = copy.getConditionEnum();
            String label = (cond != null) ? cond.getCaption()
                    : (copy.getCondition() != null ? copy.getCondition() : "-");
            com.vaadin.flow.component.html.Span span = new com.vaadin.flow.component.html.Span(
                    label != null ? label : "-");
            span.getElement().getThemeList().add("badge small contrast");
            return span;
        });
    }

    @Supply(to = "copiesDataGrid.acquisitionPrice", subject = "renderer")
    public com.vaadin.flow.data.renderer.Renderer<com.company.librarymanagementsystem.entity.BookCopy> copyAcquisitionPriceRenderer() {
        return new com.vaadin.flow.data.renderer.ComponentRenderer<>(copy -> {
            if (copy.getAcquisitionPrice() == null) {
                return new com.vaadin.flow.component.html.Span("-");
            }
            java.text.NumberFormat nf = java.text.NumberFormat
                    .getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"));
            return new com.vaadin.flow.component.html.Span(nf.format(copy.getAcquisitionPrice()) + " đ");
        });
    }

    @Supply(to = "copiesDataGrid.acquiredDate", subject = "renderer")
    public com.vaadin.flow.data.renderer.Renderer<com.company.librarymanagementsystem.entity.BookCopy> copyAcquiredDateRenderer() {
        return new com.vaadin.flow.data.renderer.ComponentRenderer<>(copy -> {
            if (copy.getAcquiredDate() == null) {
                return new com.vaadin.flow.component.html.Span("-");
            }
            return new com.vaadin.flow.component.html.Span(
                    copy.getAcquiredDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        newEntity = entityStates.isNew(getEditedEntity());
        getEditedEntity().setAuthors(new ArrayList<>(selectedAuthors));
        getEditedEntity().setCategories(new ArrayList<>(selectedCategories));
        if (getEditedEntity().getCoverImageUrl() == null || getEditedEntity().getCoverImageUrl().trim().isEmpty()) {
            getEditedEntity().setCoverImageUrl(DEFAULT_COVER_IMAGE);
        }
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        if (newEntity) {
            toastService
                    .success(String.format("Thêm mới đầu sách '%s' thành công! Bây giờ bạn có thể nhập bản sao vật lý.",
                            getEditedEntity().getTitle()));
            newEntity = false;
        } else {
            toastService.success(String.format("Cập nhật đầu sách '%s' thành công!", getEditedEntity().getTitle()));
        }
    }
}
