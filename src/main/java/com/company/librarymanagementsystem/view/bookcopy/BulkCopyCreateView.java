package com.company.librarymanagementsystem.view.bookcopy;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.BookCopyService;
import com.company.librarymanagementsystem.service.ToastService;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ViewController(id = "BulkCopy.create")
@ViewDescriptor(path = "bulk-copy-create-view.xml")
@DialogMode(width = "46em")
public class BulkCopyCreateView extends StandardView {

    @ViewComponent
    private EntityComboBox<Book> bookField;

    @ViewComponent
    private IntegerField quantityField;

    @ViewComponent
    private EntityComboBox<Location> locationField;

    @ViewComponent
    private Select<String> conditionField;

    @ViewComponent
    private DatePicker acquiredDateField;

    @ViewComponent
    private BigDecimalField acquisitionPriceField;

    @ViewComponent
    private TextField acquisitionSourceField;

    @ViewComponent
    private TextField noteField;

    @Autowired
    private BookCopyService bookCopyService;

    @Autowired
    private ToastService toastService;

    public void setInitialBook(Book book) {
        if (book != null) {
            bookField.setValue(book);
            bookField.setReadOnly(true);
            if (book.getPrice() != null) {
                acquisitionPriceField.setValue(book.getPrice());
            }
        }
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        List<String> conditions = new ArrayList<>();
        for (BookCopyCondition c : BookCopyCondition.values()) {
            conditions.add(c.getId());
        }
        conditionField.setItems(conditions);
        conditionField.setItemLabelGenerator(id -> {
            if (id == null) {
                return "";
            }
            BookCopyCondition c = BookCopyCondition.fromId(id);
            return c != null ? c.getCaption() : id;
        });
        conditionField.setValue(BookCopyCondition.NEW.getId());
        acquiredDateField.setValue(LocalDate.now());
    }

    @Subscribe("bookField")
    public void onBookFieldValueChange(final ComponentValueChangeEvent<EntityComboBox<Book>, Book> event) {
        Book selected = event.getValue();
        if (selected != null && selected.getPrice() != null && acquisitionPriceField.getValue() == null) {
            acquisitionPriceField.setValue(selected.getPrice());
        }
    }

    @Subscribe("submitButton")
    public void onSubmitButtonClick(final ClickEvent<Button> event) {
        Book book = bookField.getValue();
        if (book == null) {
            toastService.error("Vui lòng chọn Đầu sách cần nhập bản sao!");
            return;
        }

        Integer qty = quantityField.getValue();
        if (qty == null || qty <= 0) {
            toastService.error("Số lượng bản sao phải lớn hơn 0!");
            return;
        }

        Location location = locationField.getValue();
        BookCopyCondition condition = BookCopyCondition.fromId(conditionField.getValue());
        LocalDate acqDate = acquiredDateField.getValue();
        BigDecimal price = acquisitionPriceField.getValue();
        String source = acquisitionSourceField.getValue();
        String note = noteField.getValue();

        try {
            List<BookCopy> created = bookCopyService.createBulkCopies(
                    book, qty, location, condition, acqDate, price, source, note);

            toastService.success(String.format(
                    "Nhập kho thành công %d bản sao cho đầu sách '%s'!",
                    created.size(), book.getTitle()));

            close(StandardOutcome.SAVE);
        } catch (Exception e) {
            toastService.error("Có lỗi xảy ra khi tạo bản sao: " + e.getMessage());
        }
    }

    @Subscribe("cancelButton")
    public void onCancelButtonClick(final ClickEvent<Button> event) {
        close(StandardOutcome.DISCARD);
    }
}
