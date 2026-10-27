package com.company.librarymanagementsystem.view.bookcopy;

import com.company.librarymanagementsystem.entity.BookCopy;
import com.company.librarymanagementsystem.entity.Location;
import com.company.librarymanagementsystem.service.BookCopyService;
import com.company.librarymanagementsystem.service.ToastService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@ViewController(id = "BulkCopy.relocate")
@ViewDescriptor(path = "bulk-copy-relocate-view.xml")
@DialogMode(width = "40em")
public class BulkCopyRelocateView extends StandardView {

    @ViewComponent
    private Span selectedCountSpan;

    @ViewComponent
    private EntityComboBox<Location> targetLocationField;

    @Autowired
    private BookCopyService bookCopyService;

    @Autowired
    private ToastService toastService;

    private List<BookCopy> selectedCopies;

    public void setSelectedCopies(List<BookCopy> copies) {
        this.selectedCopies = copies;
        if (selectedCountSpan != null) {
            updateCountSpan();
        }
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        updateCountSpan();
    }

    private void updateCountSpan() {
        if (selectedCopies != null && selectedCountSpan != null) {
            selectedCountSpan.setText(String.format("Đang chọn %d bản sao sách để di chuyển sang vị trí kệ mới:", selectedCopies.size()));
        }
    }

    @Subscribe("submitButton")
    public void onSubmitButtonClick(final ClickEvent<Button> event) {
        Location targetLocation = targetLocationField.getValue();
        if (targetLocation == null) {
            toastService.error("Vui lòng chọn vị trí kệ đích cần chuyển đến!");
            return;
        }

        if (selectedCopies == null || selectedCopies.isEmpty()) {
            toastService.error("Không có bản sao nào được chọn!");
            return;
        }

        try {
            bookCopyService.relocateCopies(selectedCopies, targetLocation);
            toastService.success(String.format(
                    "Đã chuyển thành công %d bản sao sang kệ '%s' (%s)!",
                    selectedCopies.size(), targetLocation.getName(), targetLocation.getCode()));
            close(StandardOutcome.SAVE);
        } catch (Exception e) {
            toastService.error("Lỗi khi chuyển vị trí kệ: " + e.getMessage());
        }
    }

    @Subscribe("cancelButton")
    public void onCancelButtonClick(final ClickEvent<Button> event) {
        close(StandardOutcome.DISCARD);
    }
}
