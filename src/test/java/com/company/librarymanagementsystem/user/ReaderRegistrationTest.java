package com.company.librarymanagementsystem.user;

import com.company.librarymanagementsystem.entity.Member;
import com.company.librarymanagementsystem.entity.MemberStatus;
import com.company.librarymanagementsystem.entity.MemberType;
import com.company.librarymanagementsystem.entity.User;
import com.company.librarymanagementsystem.entity.UserStatus;
import com.company.librarymanagementsystem.service.MemberService;
import com.company.librarymanagementsystem.service.ReaderRegistrationService;
import com.company.librarymanagementsystem.service.ReaderRegistrationService.RegistrationRequest;
import com.company.librarymanagementsystem.service.ReaderRegistrationService.RegistrationResult;
import com.company.librarymanagementsystem.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class ReaderRegistrationTest {

    @Autowired
    private ReaderRegistrationService readerRegistrationService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private DataManager dataManager;

    private User registeredUser;
    private Member registeredMember;

    @Test
    void testValidation_invalidInputs() {
        // Tên đăng nhập trống
        assertThrows(IllegalArgumentException.class, () -> readerRegistrationService.validate(
                new RegistrationRequest("", "Nguyễn Văn A", "a@test.com", "0912345678", "123456", "123456", 1L)
        ));

        // Mật khẩu xác nhận không khớp
        assertThrows(IllegalArgumentException.class, () -> readerRegistrationService.validate(
                new RegistrationRequest("testreader", "Nguyễn Văn A", "a@test.com", "0912345678", "123456", "different", 1L)
        ));

        // Email sai định dạng
        assertThrows(IllegalArgumentException.class, () -> readerRegistrationService.validate(
                new RegistrationRequest("testreader", "Nguyễn Văn A", "invalid-email", "0912345678", "123456", "123456", 1L)
        ));

        // Chưa chọn loại đối tượng độc giả
        assertThrows(IllegalArgumentException.class, () -> readerRegistrationService.validate(
                new RegistrationRequest("testreader", "Nguyễn Văn A", "valid@test.com", "0912345678", "123456", "123456", null)
        ));
    }

    @Test
    void testRegisterReader_withMemberType_andPendingStatus() {
        // 1. Tải loại độc giả STUDENT từ database
        MemberType studentType = dataManager.load(MemberType.class)
                .query("select mt from MemberType mt where mt.code = 'STUDENT' and mt.deletedAt is null")
                .optional()
                .orElseGet(() -> {
                    MemberType mt = dataManager.create(MemberType.class);
                    mt.setCode("STUDENT");
                    mt.setName("Sinh viên");
                    mt.setMaxLoanBooks(5);
                    mt.setLoanDays(14);
                    mt.setCardValidMonths(12);
                    return dataManager.save(mt);
                });

        String uniqueSuffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String username = "reader_" + uniqueSuffix;
        String email = "reader_" + uniqueSuffix + "@library.test";
        String phone = "09" + uniqueSuffix.substring(0, Math.min(8, uniqueSuffix.length()));
        while (phone.length() < 10) {
            phone += "0";
        }

        RegistrationRequest request = new RegistrationRequest(
                username,
                "Độc Giả Thử Nghiệm",
                email,
                phone,
                "Password123@",
                "Password123@",
                studentType.getId()
        );

        RegistrationResult result = readerRegistrationService.registerReader(request);

        assertThat(result.success()).isTrue();
        assertThat(result.username()).isEqualTo(username);
        assertThat(result.memberCode()).isNotBlank();
        assertThat(result.memberTypeName()).isEqualTo("Sinh viên");
        assertThat(result.memberStatus()).isEqualTo(MemberStatus.PENDING.getId());

        // Kiểm tra User trong database
        Optional<User> loadedUserOpt = dataManager.load(User.class)
                .query("select u from User u where u.username = :uname")
                .parameter("uname", username)
                .optional();
        assertThat(loadedUserOpt).isPresent();
        registeredUser = loadedUserOpt.get();

        assertThat(registeredUser.getRole()).isNotNull();
        assertThat(registeredUser.getRole().getCode()).isEqualTo("READER");
        assertThat(registeredUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(registeredUser.getActive()).isTrue();

        // Kiểm tra Member trong database
        Optional<Member> loadedMemberOpt = dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.user = :u")
                .parameter("u", registeredUser)
                .optional();
        assertThat(loadedMemberOpt).isPresent();
        registeredMember = loadedMemberOpt.get();

        assertThat(registeredMember.getMemberCode()).isEqualTo(result.memberCode());
        assertThat(registeredMember.getFullName()).isEqualTo("Độc Giả Thử Nghiệm");
        assertThat(registeredMember.getMemberType()).isNotNull();
        assertThat(registeredMember.getMemberType().getId()).isEqualTo(studentType.getId());

        // Thẻ phải ở trạng thái CHỜ DUYỆT (PENDING) khi đăng ký trực tuyến
        assertThat(registeredMember.getStatus()).isEqualTo(MemberStatus.PENDING.getId());

        // Độc giả chưa được phép mượn sách khi thẻ chưa được Thủ thư kích hoạt tại quầy
        MemberService.EligibilityResult eligibilityBefore = memberService.checkEligibility(registeredMember);
        assertThat(eligibilityBefore.isEligible()).isFalse();
        assertThat(eligibilityBefore.getBlockingReasons()).anyMatch(r -> r.contains("CHỜ DUYỆT"));

        // Khi độc giả mang giấy tờ đến quầy, Thủ thư duyệt cấp thẻ
        registeredMember = memberService.activateMember(registeredMember);
        assertThat(registeredMember.getStatus()).isEqualTo(MemberStatus.ACTIVE.getId());
        assertThat(registeredMember.getCardIssuedDate()).isEqualTo(LocalDate.now());

        // Sau khi được duyệt, độc giả đủ tư cách mượn sách
        MemberService.EligibilityResult eligibilityAfter = memberService.checkEligibility(registeredMember);
        assertThat(eligibilityAfter.isEligible()).isTrue();
    }

    @AfterEach
    void tearDown() {
        if (registeredMember != null) {
            dataManager.load(Member.class).id(registeredMember.getId()).optional().ifPresent(dataManager::remove);
        }
        if (registeredUser != null) {
            dataManager.load(User.class).id(registeredUser.getId()).optional().ifPresent(dataManager::remove);
        }
    }
}
