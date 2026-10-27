package com.company.librarymanagementsystem.book;

import com.company.librarymanagementsystem.LibraryManagementSystemApplication;
import com.company.librarymanagementsystem.entity.Book;
import com.company.librarymanagementsystem.service.DatabaseSequenceSynchronizer;
import com.company.librarymanagementsystem.view.book.BookDetailView;
import com.company.librarymanagementsystem.view.book.BookListView;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.FlowuiTestAssistConfiguration;
import io.jmix.flowui.testassist.UiTest;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.view.View;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@UiTest
@SpringBootTest(classes = {LibraryManagementSystemApplication.class, FlowuiTestAssistConfiguration.class})
public class BookCreateUiTest {

    @Autowired
    ViewNavigators viewNavigators;

    @Autowired
    DatabaseSequenceSynchronizer sequenceSynchronizer;

    @Test
    void test_bookCreate_formMustBeCompletelyEmptyForManualInput() {
        // Run sequence synchronizer to ensure sequence is above seed data
        sequenceSynchronizer.synchronizeSequences();

        viewNavigators.view(UiTestUtils.getCurrentView(), BookListView.class).navigate();
        BookListView bookListView = UiTestUtils.getCurrentView();

        JmixButton createBtn = UiTestUtils.getComponent(bookListView, "createButton");
        createBtn.click();

        View<?> currentView = UiTestUtils.getCurrentView();
        System.out.println("DEBUG view: " + currentView.getClass() + ", id=" + currentView.getId() + ", equals=" + (currentView.getClass().getName().equals(BookDetailView.class.getName())));
        Assertions.assertEquals("Book.detail", currentView.getId().orElse(""));

        io.jmix.flowui.view.StandardDetailView<?> detailView = (io.jmix.flowui.view.StandardDetailView<?>) currentView;
        Book edited = (Book) detailView.getEditedEntity();
        assertNotNull(edited, "Edited book entity must not be null");

        // The ID must be safe (> 180 seed books)
        assertTrue(edited.getId() > 180, "Generated book ID must be > 180 to avoid collision with seed books. Actual: " + edited.getId());

        // All core fields must be empty / null for manual input
        assertNull(edited.getTitle(), "Title must be empty");
        assertNull(edited.getIsbn(), "ISBN must be empty");
        assertNull(edited.getDdc(), "DDC must be empty");
        assertNull(edited.getPublisher(), "Publisher must be empty");
        assertTrue(edited.getAuthors() == null || edited.getAuthors().isEmpty(), "Authors must be empty");
        assertTrue(edited.getCategories() == null || edited.getCategories().isEmpty(), "Categories must be empty");
        assertNull(edited.getPrice(), "Price must be empty");
        assertNull(edited.getPageCount(), "Page count must be empty");
        assertNull(edited.getPublishYear(), "Publish year must be empty");

        // UI components must also be empty
        TypedTextField<String> titleField = UiTestUtils.getComponent(detailView, "titleField");
        assertTrue(titleField.getValue() == null || titleField.getValue().trim().isEmpty(), "titleField UI component must be empty");

        TypedTextField<String> isbnField = UiTestUtils.getComponent(detailView, "isbnField");
        assertTrue(isbnField.getValue() == null || isbnField.getValue().trim().isEmpty(), "isbnField UI component must be empty");
    }
}
