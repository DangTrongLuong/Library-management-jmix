package com.company.librarymanagementsystem.publisher;

import com.company.librarymanagementsystem.LibraryManagementSystemApplication;
import com.company.librarymanagementsystem.entity.Publisher;
import com.company.librarymanagementsystem.view.publisher.PublisherDetailView;
import com.company.librarymanagementsystem.view.publisher.PublisherListView;
import io.jmix.core.DataManager;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.FlowuiTestAssistConfiguration;
import io.jmix.flowui.testassist.UiTest;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.view.View;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@UiTest
@SpringBootTest(classes = {LibraryManagementSystemApplication.class, FlowuiTestAssistConfiguration.class})
public class PublisherUiTest {

    @Autowired
    DataManager dataManager;

    @Autowired
    ViewNavigators viewNavigators;

    private Long createdPublisherId;

    @Test
    void test_publishersSeededAndCreateNew() {
        // Navigate to publisher list view
        viewNavigators.view(UiTestUtils.getCurrentView(), PublisherListView.class).navigate();

        PublisherListView publisherListView = UiTestUtils.getCurrentView();

        long totalInDb = dataManager.load(Publisher.class).all().list().size();
        Assertions.assertTrue(totalInDb >= 62, "Seeded publishers in DB should be at least 62");

        DataGrid<Publisher> dataGrid = UiTestUtils.getComponent(publisherListView, "publishersDataGrid");
        DataGridItems<Publisher> items = dataGrid.getItems();
        Assertions.assertNotNull(items);
        Assertions.assertTrue(items.getItems().size() > 0, "DataGrid should display items on first page");

        // Click create button
        JmixButton createBtn = UiTestUtils.getComponent(publisherListView, "createButton");
        createBtn.click();

        // Get detail view
        View<?> detailView = UiTestUtils.getCurrentView();
        Assertions.assertTrue(detailView instanceof PublisherDetailView);

        // Fill in name and address
        String uniqueName = "NXB Test " + System.currentTimeMillis();
        TypedTextField<String> nameField = UiTestUtils.getComponent(detailView, "nameField");
        nameField.setValue(uniqueName);

        TypedTextField<String> addressField = UiTestUtils.getComponent(detailView, "addressField");
        addressField.setValue("123 Phố Test, Hà Nội");

        // Save and close
        JmixButton saveAndCloseBtn = UiTestUtils.getComponent(detailView, "saveAndCloseButton");
        saveAndCloseBtn.click();

        // Verify in DB
        Optional<Publisher> created = dataManager.load(Publisher.class)
                .query("select p from Publisher p where p.name = :name")
                .parameter("name", uniqueName)
                .optional();

        Assertions.assertTrue(created.isPresent(), "Newly created publisher should be in DB");
        createdPublisherId = created.get().getId();
    }

    @AfterEach
    void tearDown() {
        if (createdPublisherId != null) {
            dataManager.load(Publisher.class).id(createdPublisherId).optional()
                    .ifPresent(p -> dataManager.remove(p));
        }
    }
}
