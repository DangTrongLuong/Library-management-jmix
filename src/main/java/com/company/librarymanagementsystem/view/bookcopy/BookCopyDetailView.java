package com.company.librarymanagementsystem.view.bookcopy;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.BookCopyService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Route(value = "book-copies/:id", layout = MainView.class)
@ViewController(id = "BookCopy.detail")
@ViewDescriptor(path = "book-copy-detail-view.xml")
@EditedEntityContainer("bookCopyDc")
@DialogMode(width = "48em")
public class BookCopyDetailView extends StandardDetailView<BookCopy> {

    @ViewComponent
    private EntityComboBox<Book> bookField;

    @ViewComponent
    private TextField barcodeField;

    @ViewComponent
    private Select<String> statusField;

    @ViewComponent
    private Select<String> conditionField;

    @Autowired
    private BookCopyService bookCopyService;

    @Autowired
    private ToastService toastService;

    @Subscribe
    public void onInit(final InitEvent event) {
        List<String> statuses = new ArrayList<>();
        for (BookCopyStatus s : BookCopyStatus.values()) {
            statuses.add(s.getId());
        }
        statusField.setItems(statuses);
        statusField.setItemLabelGenerator(id -> {
            if (id == null) {
                return "";
            }
            BookCopyStatus s = BookCopyStatus.fromId(id);
            return s != null ? s.getCaption() : id;
        });

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
    }

    @Subscribe
    public void onInitEntity(final InitEntityEvent<BookCopy> event) {
        event.getEntity().setStatus(BookCopyStatus.AVAILABLE.getId());
        event.getEntity().setCondition(BookCopyCondition.GOOD.getId());
        event.getEntity().setAcquiredDate(LocalDate.now());
    }

    @Subscribe("generateBarcodeBtn")
    public void onGenerateBarcodeBtnClick(final ClickEvent<Button> event) {
        Book selectedBook = bookField.getValue();
        if (selectedBook == null) {
            toastService.warning("Vui lòng chọn Đầu sách trước khi sinh mã vạch!");
            return;
        }

        String barcode = bookCopyService.generateNextBarcode(selectedBook);
        barcodeField.setValue(barcode);
        toastService.info("Đã sinh mã vạch: " + barcode);
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        toastService.success("Lưu bản sao sách thành công!");
    }
}
