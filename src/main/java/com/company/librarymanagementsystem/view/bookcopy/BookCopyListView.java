package com.company.librarymanagementsystem.view.bookcopy;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Route(value = "book-copies", layout = MainView.class)
@ViewController(id = "BookCopy.list")
@ViewDescriptor(path = "book-copy-list-view.xml")
@LookupComponent("bookCopiesDataGrid")
@DialogMode(width = "64em")
public class BookCopyListView extends StandardListView<BookCopy> {

    @ViewComponent
    private CollectionContainer<BookCopy> bookCopiesDc;

    @ViewComponent
    private CollectionLoader<BookCopy> bookCopiesDl;

    @ViewComponent
    private DataGrid<BookCopy> bookCopiesDataGrid;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private Select<String> statusFilter;

    @ViewComponent
    private EntityComboBox<Location> locationFilter;

    @ViewComponent
    private Button bulkCreateButton;

    @ViewComponent
    private Button createButton;

    @ViewComponent
    private Button printBarcodeButton;

    @ViewComponent
    private Button editButton;

    @ViewComponent
    private Button removeButton;

    @Autowired
    private ToastService toastService;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private com.company.librarymanagementsystem.security.AccessControlService accessControlService;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        accessControlService.applyPermission(bulkCreateButton, com.company.librarymanagementsystem.security.Permissions.COPY_CREATE);
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.COPY_CREATE);
        accessControlService.applyPermission(bookCopiesDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.COPY_CREATE);

        accessControlService.applyPermission(printBarcodeButton, com.company.librarymanagementsystem.security.Permissions.COPY_VIEW);

        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.COPY_EDIT);
        accessControlService.applyPermission(bookCopiesDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.COPY_EDIT);

        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.COPY_WITHDRAW);
        accessControlService.applyPermission(bookCopiesDataGrid.getAction("removeAction"), com.company.librarymanagementsystem.security.Permissions.COPY_WITHDRAW);
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);

        List<String> statuses = new ArrayList<>();
        for (BookCopyStatus s : BookCopyStatus.values()) {
            statuses.add(s.getId());
        }
        statusFilter.setItems(statuses);
        statusFilter.setItemLabelGenerator(id -> {
            if (id == null) {
                return "";
            }
            BookCopyStatus s = BookCopyStatus.fromId(id);
            return s != null ? s.getCaption() : id;
        });

        locationFilter.setItemLabelGenerator(loc -> {
            if (loc == null) {
                return "";
            }
            String dn = loc.getDisplayName();
            return dn != null ? dn : "";
        });
    }

    @Subscribe("searchField")
    public void onSearchFieldValueChange(final ComponentValueChangeEvent<TextField, String> event) {
        applyFilters();
    }

    @Subscribe("statusFilter")
    public void onStatusFilterValueChange(final ComponentValueChangeEvent<Select<String>, String> event) {
        applyFilters();
    }

    @Subscribe("locationFilter")
    public void onLocationFilterValueChange(final ComponentValueChangeEvent<EntityComboBox<Location>, Location> event) {
        applyFilters();
    }

    @Subscribe("clearFilterBtn")
    public void onClearFilterBtnClick(final ClickEvent<Button> event) {
        searchField.clear();
        statusFilter.clear();
        locationFilter.clear();
        applyFilters();
    }

    private void applyFilters() {
        StringBuilder query = new StringBuilder("select e from BookCopy e where e.deletedAt is null");
        Map<String, Object> params = new HashMap<>();

        String search = searchField.getValue();
        if (search != null && !search.trim().isEmpty()) {
            query.append(" and (lower(e.barcode) like concat('%', lower(:search), '%') ")
                 .append(" or lower(e.book.title) like concat('%', lower(:search), '%') ")
                 .append(" or lower(e.book.isbn) like concat('%', lower(:search), '%'))");
            params.put("search", search.trim());
        }

        String status = statusFilter.getValue();
        if (status != null && !status.isEmpty()) {
            query.append(" and e.status = :status");
            params.put("status", status);
        }

        Location loc = locationFilter.getValue();
        if (loc != null) {
            query.append(" and e.location = :loc");
            params.put("loc", loc);
        }

        query.append(" order by e.id desc");

        bookCopiesDl.setQuery(query.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            bookCopiesDl.setParameter(entry.getKey(), entry.getValue());
        }
        bookCopiesDl.load();
    }

    @Subscribe("bulkCreateButton")
    public void onBulkCreateButtonClick(final ClickEvent<Button> event) {
        dialogWindows.view(this, BulkCopyCreateView.class)
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        bookCopiesDl.load();
                    }
                })
                .open();
    }

    @Subscribe("printBarcodeButton")
    public void onPrintBarcodeButtonClick(final ClickEvent<Button> event) {
        Set<BookCopy> selected = bookCopiesDataGrid.getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            toastService.warning("Vui lòng chọn ít nhất một bản sao sách để in mã vạch!");
            return;
        }

        DialogWindow<BarcodePrintView> window = dialogWindows.view(this, BarcodePrintView.class)
                .build();
        window.getView().setCopiesToPrint(new ArrayList<>(selected));
        window.open();
    }

    @Install(to = "bookCopiesDataGrid.removeAction", subject = "beforeActionPerformedHandler")
    public void bookCopiesDataGridRemoveBeforeActionPerformed(final RemoveOperation.BeforeActionPerformedEvent<BookCopy> event) {
        for (BookCopy copy : event.getItems()) {
            if ("BORROWED".equals(copy.getStatus())) {
                event.preventAction();
                toastService.error(String.format("Không thể xóa bản sao '%s' vì đang trong trạng thái ĐANG MƯỢN!", copy.getBarcode()));
                return;
            }
        }
    }

    @Install(to = "bookCopiesDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void bookCopiesDataGridRemoveAfterActionPerformed(final RemoveOperation.AfterActionPerformedEvent<BookCopy> event) {
        toastService.success("Xóa bản sao sách thành công!");
    }

    @Supply(to = "bookCopiesDataGrid.barcode", subject = "renderer")
    public Renderer<BookCopy> barcodeRenderer() {
        return new ComponentRenderer<>(copy -> {
            String b = (copy != null && copy.getBarcode() != null) ? copy.getBarcode() : "-";
            Span span = new Span(b);
            span.getElement().getThemeList().add("badge");
            span.getStyle().set("font-family", "monospace");
            span.getStyle().set("font-weight", "bold");
            span.getStyle().set("font-size", "0.95rem");
            return span;
        });
    }

    @Supply(to = "bookCopiesDataGrid.status", subject = "renderer")
    public Renderer<BookCopy> statusRenderer() {
        return new ComponentRenderer<>(copy -> {
            if (copy == null) {
                return new Span("-");
            }
            BookCopyStatus status = copy.getStatusEnum();
            String label = (status != null) ? status.getCaption() : (copy.getStatus() != null ? copy.getStatus() : "-");
            Span span = new Span(label != null ? label : "-");
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

    @Supply(to = "bookCopiesDataGrid.condition", subject = "renderer")
    public Renderer<BookCopy> conditionRenderer() {
        return new ComponentRenderer<>(copy -> {
            if (copy == null) {
                return new Span("-");
            }
            BookCopyCondition cond = copy.getConditionEnum();
            String label = (cond != null) ? cond.getCaption() : (copy.getCondition() != null ? copy.getCondition() : "-");
            Span span = new Span(label != null ? label : "-");
            span.getElement().getThemeList().add("badge small contrast");
            return span;
        });
    }

    @Supply(to = "bookCopiesDataGrid.location", subject = "renderer")
    public Renderer<BookCopy> locationRenderer() {
        return new ComponentRenderer<>(copy -> {
            if (copy == null || copy.getLocation() == null) {
                Span empty = new Span("Chưa xếp kệ");
                empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
                empty.getStyle().set("font-style", "italic");
                return empty;
            }
            String disp = copy.getLocation().getDisplayName();
            Span span = new Span(disp != null ? disp : "-");
            span.getStyle().set("font-weight", "500");
            return span;
        });
    }

    @Supply(to = "bookCopiesDataGrid.acquisitionPrice", subject = "renderer")
    public Renderer<BookCopy> acquisitionPriceRenderer() {
        return new ComponentRenderer<>(copy -> {
            if (copy == null || copy.getAcquisitionPrice() == null) {
                return new Span("-");
            }
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
            return new Span(nf.format(copy.getAcquisitionPrice()) + " đ");
        });
    }

    @Supply(to = "bookCopiesDataGrid.acquiredDate", subject = "renderer")
    public Renderer<BookCopy> acquiredDateRenderer() {
        return new ComponentRenderer<>(copy -> {
            if (copy == null || copy.getAcquiredDate() == null) {
                return new Span("-");
            }
            return new Span(copy.getAcquiredDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
    }
}
