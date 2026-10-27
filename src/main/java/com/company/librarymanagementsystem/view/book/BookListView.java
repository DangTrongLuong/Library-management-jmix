package com.company.librarymanagementsystem.view.book;

import com.company.librarymanagementsystem.entity.Author;
import com.company.librarymanagementsystem.entity.Book;
import com.company.librarymanagementsystem.entity.Category;
import com.company.librarymanagementsystem.security.AccessControlService;
import com.company.librarymanagementsystem.security.Permissions;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "books", layout = MainView.class)
@ViewController(id = "Book.list")
@ViewDescriptor(path = "book-list-view.xml")
@LookupComponent("booksDataGrid")
@DialogMode(width = "70em")
public class BookListView extends StandardListView<Book> {

    @ViewComponent
    private CollectionContainer<Book> booksDc;

    @ViewComponent
    private CollectionLoader<Book> booksDl;

    @ViewComponent
    private DataGrid<Book> booksDataGrid;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private Button createButton;

    @ViewComponent
    private Button editButton;

    @ViewComponent
    private Button removeButton;

    @Autowired
    private ToastService toastService;

    @Autowired
    private AccessControlService accessControlService;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

    @Subscribe
    public void onInit(final InitEvent event) {
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        accessControlService.applyPermission(createButton, Permissions.BOOK_CREATE);
        accessControlService.applyPermission(booksDataGrid.getAction("createAction"), Permissions.BOOK_CREATE);

        accessControlService.applyPermission(editButton, Permissions.BOOK_EDIT);
        accessControlService.applyPermission(booksDataGrid.getAction("editAction"), Permissions.BOOK_EDIT);

        accessControlService.applyPermission(removeButton, Permissions.BOOK_DELETE);
        accessControlService.applyPermission(booksDataGrid.getAction("removeAction"), Permissions.BOOK_DELETE);
    }

    @Subscribe("searchField")
    public void onSearchFieldValueChange(final ComponentValueChangeEvent<TextField, String> event) {
        String filter = event.getValue();
        if (filter != null && !filter.trim().isEmpty()) {
            booksDl.setQuery("select e from Book e where lower(e.title) like concat('%', lower(:search), '%') " +
                    "or lower(e.isbn) like concat('%', lower(:search), '%') " +
                    "or lower(e.ddc) like concat('%', lower(:search), '%') " +
                    "or lower(e.subtitle) like concat('%', lower(:search), '%') " +
                    "order by e.title");
            booksDl.setParameter("search", filter.trim());
        } else {
            booksDl.setQuery("select e from Book e order by e.title");
            booksDl.removeParameter("search");
        }
        booksDl.load();
    }

    @Autowired
    private io.jmix.core.DataManager dataManager;

    @Install(to = "booksDataGrid.removeAction", subject = "beforeActionPerformedHandler")
    public void booksDataGridRemoveBeforeActionPerformed(final RemoveOperation.BeforeActionPerformedEvent<Book> event) {
        for (Book book : event.getItems()) {
            Long copyCount = dataManager.loadValue(
                            "select count(c) from BookCopy c where c.book = :book and c.deletedAt is null", Long.class)
                    .parameter("book", book)
                    .one();
            if (copyCount != null && copyCount > 0) {
                event.preventAction();
                toastService.error(String.format("Không thể xóa đầu sách '%s' vì hiện đang còn %d bản sao trong kho!", book.getTitle(), copyCount));
                return;
            }
        }
    }

    @Supply(to = "booksDataGrid.coverImage", subject = "renderer")
    public Renderer<Book> coverImageRenderer() {
        return new ComponentRenderer<>(book -> {
            Image img = new Image();
            img.setWidth("42px");
            img.setHeight("56px");
            img.getStyle().set("object-fit", "cover");
            img.getStyle().set("border-radius", "4px");
            img.getStyle().set("box-shadow", "0 1px 3px rgba(0,0,0,0.15)");
            img.getStyle().set("display", "block");
            img.getStyle().set("background-color", "var(--lumo-contrast-5pct)");

            String url = book.getCoverImageUrl();
            if (url != null && !url.trim().isEmpty()) {
                img.setSrc(url.trim());
                img.setAlt(book.getTitle() != null ? book.getTitle() : "Bìa sách");
            } else {
                img.setVisible(false);
            }
            return img;
        });
    }

    @Supply(to = "booksDataGrid.copiesCountInfo", subject = "renderer")
    public Renderer<Book> copiesCountInfoRenderer() {
        return new ComponentRenderer<>(book -> {
            long available = book.getAvailableCopies() != null ? book.getAvailableCopies() : 0L;
            int total = book.getTotalCopies() != null ? book.getTotalCopies() : 0;
            Span span = new Span(String.format("%d / %d", available, total));
            span.getElement().getThemeList().add("badge");
            if (total == 0) {
                span.setText("Chưa có bản sao");
                span.getElement().getThemeList().add("contrast small");
            } else if (available > 0) {
                span.getElement().getThemeList().add("success");
            } else {
                span.getElement().getThemeList().add("error");
            }
            return span;
        });
    }

    @Install(to = "booksDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void booksDataGridRemoveAfterActionPerformed(final RemoveOperation.AfterActionPerformedEvent<Book> event) {
        toastService.success("Xóa đầu sách thành công!");
    }

    @Supply(to = "booksDataGrid.isbn", subject = "renderer")
    public Renderer<Book> isbnRenderer() {
        return new ComponentRenderer<>(book -> {
            if (book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {
                return new Span(book.getIsbn());
            }
            Span empty = new Span("—");
            empty.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            return empty;
        });
    }

    @Supply(to = "booksDataGrid.ddc", subject = "renderer")
    public Renderer<Book> ddcRenderer() {
        return new ComponentRenderer<>(book -> {
            if (book == null || book.getDdc() == null || book.getDdc().trim().isEmpty()) {
                Span empty = new Span("—");
                empty.getStyle().set("color", "var(--lumo-tertiary-text-color)");
                return empty;
            }
            Span span = new Span(book.getDdc().trim());
            span.getElement().getThemeList().add("badge contrast small");
            span.getStyle().set("font-family", "monospace").set("font-weight", "bold");
            return span;
        });
    }

    @Supply(to = "booksDataGrid.title", subject = "renderer")
    public Renderer<Book> titleRenderer() {
        return new ComponentRenderer<>(book -> {
            String title = book.getTitle() != null ? book.getTitle() : "";
            Span titleSpan = new Span(title);
            titleSpan.getStyle().set("font-weight", "600");
            titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            titleSpan.getStyle().set("overflow", "hidden");
            titleSpan.getStyle().set("text-overflow", "ellipsis");
            titleSpan.getStyle().set("white-space", "nowrap");
            titleSpan.getStyle().set("display", "block");
            titleSpan.getStyle().set("max-width", "22em");
            titleSpan.setTitle(title);
            return titleSpan;
        });
    }

    @Supply(to = "booksDataGrid.authors", subject = "renderer")
    public Renderer<Book> authorsRenderer() {
        return new ComponentRenderer<>(book -> {
            if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                String authorsStr = book.getAuthors().stream()
                        .map(Author::getFullName)
                        .collect(Collectors.joining(", "));
                return new Span(authorsStr);
            }
            Span empty = new Span("—");
            empty.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            return empty;
        });
    }

    @Supply(to = "booksDataGrid.categories", subject = "renderer")
    public Renderer<Book> categoriesRenderer() {
        return new ComponentRenderer<>(book -> {
            List<Category> categories = book.getCategories();
            if (categories != null && !categories.isEmpty()) {
                VerticalLayout container = new VerticalLayout();
                container.setPadding(false);
                container.setSpacing(false);
                container.setWidth(null);
                container.getStyle().set("gap", "4px");
                container.getStyle().set("padding", "3px 0");

                for (int i = 0; i < categories.size(); i += 2) {
                    HorizontalLayout row = new HorizontalLayout();
                    row.setPadding(false);
                    row.setSpacing(true);
                    row.setAlignItems(FlexComponent.Alignment.CENTER);
                    row.getStyle().set("gap", "4px");

                    Span badge1 = new Span(categories.get(i).getName());
                    badge1.getElement().getThemeList().add("badge");
                    badge1.getStyle().set("white-space", "nowrap");
                    row.add(badge1);

                    if (i + 1 < categories.size()) {
                        Span badge2 = new Span(categories.get(i + 1).getName());
                        badge2.getElement().getThemeList().add("badge");
                        badge2.getStyle().set("white-space", "nowrap");
                        row.add(badge2);
                    }
                    container.add(row);
                }
                return container;
            }
            Span empty = new Span("—");
            empty.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            return empty;
        });
    }

    @Supply(to = "booksDataGrid.publisher", subject = "renderer")
    public Renderer<Book> publisherRenderer() {
        return new ComponentRenderer<>(book -> {
            if (book.getPublisher() != null) {
                return new Span(book.getPublisher().getName());
            }
            Span empty = new Span("—");
            empty.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            return empty;
        });
    }

    @Supply(to = "booksDataGrid.publishYear", subject = "renderer")
    public Renderer<Book> publishYearRenderer() {
        return new ComponentRenderer<>(book -> {
            String year = book.getPublishYear();
            Span span = new Span(year != null && !year.trim().isEmpty() ? year : "—");
            span.getStyle().set("font-weight", "500");
            span.getStyle().set("text-align", "center");
            span.getStyle().set("display", "block");
            span.getStyle().set("width", "100%");
            return span;
        });
    }

    @Supply(to = "booksDataGrid.price", subject = "renderer")
    public Renderer<Book> priceRenderer() {
        return new ComponentRenderer<>(book -> {
            Span span = new Span(book.getPrice() != null ? currencyFormat.format(book.getPrice()) + " đ" : "0 đ");
            span.getStyle().set("font-weight", "500");
            span.getStyle().set("text-align", "right");
            span.getStyle().set("display", "block");
            span.getStyle().set("width", "100%");
            return span;
        });
    }

    @Supply(to = "booksDataGrid.status", subject = "renderer")
    public Renderer<Book> statusRenderer() {
        return new ComponentRenderer<>(book -> {
            Span badge = new Span();
            String status = book.getStatus();
            if ("ACTIVE".equalsIgnoreCase(status)) {
                badge.setText("Đang lưu hành");
                badge.getElement().getThemeList().add("badge success");
            } else if ("HIDDEN".equalsIgnoreCase(status)) {
                badge.setText("Tạm ẩn");
                badge.getElement().getThemeList().add("badge contrast");
            } else {
                badge.setText(status != null ? status : "—");
                badge.getElement().getThemeList().add("badge");
            }
            HorizontalLayout wrapper = new HorizontalLayout(badge);
            wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            wrapper.setWidthFull();
            wrapper.setPadding(false);
            wrapper.setSpacing(false);
            return wrapper;
        });
    }
}
