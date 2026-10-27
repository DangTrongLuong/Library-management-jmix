package com.company.librarymanagementsystem.view.member;

import com.company.librarymanagementsystem.entity.Member;
import com.company.librarymanagementsystem.entity.MemberStatus;
import com.company.librarymanagementsystem.service.MemberService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Arrays;

@Route(value = "members/:id", layout = MainView.class)
@ViewController(id = "LibraryMember.detail")
@ViewDescriptor(path = "member-detail-view.xml")
@EditedEntityContainer("memberDc")
@DialogMode(width = "56em")
public class MemberDetailView extends StandardDetailView<Member> {

    @ViewComponent
    private ComboBox<String> genderField;

    @ViewComponent
    private ComboBox<String> statusField;

    @ViewComponent
    private TypedDatePicker<LocalDate> cardIssuedDateField;

    @ViewComponent
    private TypedDatePicker<LocalDate> cardExpiryDateField;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ToastService toastService;

    @Autowired
    private EntityStates entityStates;

    @Subscribe
    public void onInit(final InitEvent event) {
        setShowSaveNotification(false);

        genderField.setItems(Arrays.asList("MALE", "FEMALE", "OTHER"));
        genderField.setItemLabelGenerator(g -> switch (g) {
            case "MALE" -> "Nam";
            case "FEMALE" -> "Nữ";
            case "OTHER" -> "Khác";
            default -> g;
        });

        statusField.setItems(Arrays.asList("ACTIVE", "SUSPENDED", "BLOCKED", "EXPIRED", "PENDING"));
        statusField.setItemLabelGenerator(s -> switch (s) {
            case "ACTIVE" -> "Đang hoạt động";
            case "SUSPENDED" -> "Tạm khóa";
            case "BLOCKED" -> "Bị khóa vĩnh viễn";
            case "EXPIRED" -> "Hết hạn";
            case "PENDING" -> "Chờ duyệt";
            default -> s;
        });
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        Member m = getEditedEntity();
        boolean isNew = entityStates.isNew(m);

        if (isNew) {
            String code = memberService.generateNextMemberCode();
            m.setMemberCode(code);

            if (m.getCardIssuedDate() == null) {
                m.setCardIssuedDate(LocalDate.now());
                cardIssuedDateField.setTypedValue(LocalDate.now());
            }

            if (m.getCardExpiryDate() == null) {
                m.setCardExpiryDate(LocalDate.now().plusYears(1));
                cardExpiryDateField.setTypedValue(LocalDate.now().plusYears(1));
            }

            if (m.getStatus() == null) {
                m.setStatus(MemberStatus.ACTIVE.getId());
            }
        }

        if (m.getGender() != null) {
            genderField.setValue(m.getGender());
        }

        if (m.getStatus() != null) {
            statusField.setValue(m.getStatus());
        }
    }

    @Subscribe("genderField")
    public void onGenderFieldValueChange(
            final com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<ComboBox<String>, String> event) {
        getEditedEntity().setGender(event.getValue());
    }

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        Member m = getEditedEntity();

        if (m.getFullName() == null || m.getFullName().trim().isEmpty()) {
            event.getErrors().add("Vui lòng nhập tên độc giả!");
        }

        if (m.getMemberType() == null) {
            event.getErrors().add("Vui lòng chọn loại độc giả!");
        }

        if (m.getCardIssuedDate() == null) {
            event.getErrors().add("Ngày cấp thẻ không được để trống!");
        }

        if (m.getCardExpiryDate() == null) {
            event.getErrors().add("Ngày hết hạn thẻ không được để trống!");
        }

        if (m.getCardIssuedDate() != null && m.getCardExpiryDate() != null
                && m.getCardExpiryDate().isBefore(m.getCardIssuedDate())) {
            event.getErrors().add("Ngày hết hạn thẻ phải sau ngày cấp thẻ!");
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        Member m = getEditedEntity();
        if (entityStates.isNew(m) && m.getMemberType() != null
                && m.getMemberType().getCardValidMonths() != null
                && m.getCardIssuedDate() != null) {
            LocalDate expiry = m.getCardIssuedDate().plusMonths(m.getMemberType().getCardValidMonths());
            m.setCardExpiryDate(expiry);
        }
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        Member m = getEditedEntity();
        memberService.syncUserFromMember(m);
        toastService.success(String.format("Lưu thông tin độc giả thành công! Độc giả: %s (Mã thẻ: %s)",
                m.getFullName(), m.getMemberCode()));
    }
}