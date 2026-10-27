package com.company.librarymanagementsystem.user;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.UserService;
import com.company.librarymanagementsystem.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class UserCascadeDeleteTest {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testCascadeDeleteUserWithMemberAndLoans() {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis()).substring(7);

        // 1. Tạo User
        User user = dataManager.create(User.class);
        user.setUsername("cascade_user_" + uniqueSuffix);
        user.setPassword(passwordEncoder.encode("123456a@"));
        user.setFullName("Cascade Test User");
        user.setEmail("cascade_" + uniqueSuffix + "@test.com");
        user = dataManager.save(user);

        // 2. Tạo MemberType
        MemberType memberType = dataManager.load(MemberType.class)
                .query("select mt from MemberType mt")
                .maxResults(1)
                .one();

        // 3. Tạo Member liên kết với User
        Member member = dataManager.create(Member.class);
        member.setUser(user);
        member.setMemberCode("MC-" + uniqueSuffix);
        member.setFullName("Cascade Member " + uniqueSuffix);
        member.setMemberType(memberType);
        member.setStatus("ACTIVE");
        member.setCardIssuedDate(LocalDate.now());
        member.setCardExpiryDate(LocalDate.now().plusYears(1));
        member = dataManager.save(member);

        // 4. Lấy một đầu sách và bản sao
        Book book = dataManager.load(Book.class)
                .query("select b from Book b")
                .maxResults(1)
                .one();

        BookCopy copy = dataManager.create(BookCopy.class);
        copy.setBook(book);
        copy.setBarcode("BC-TEST-" + uniqueSuffix);
        copy.setStatus(BookCopyStatus.BORROWED.getId());
        copy = dataManager.save(copy);

        // 5. Tạo Loan và LoanItem
        User adminUser = dataManager.load(User.class)
                .query("select u from User u where lower(u.username) = 'admin'")
                .one();

        Loan loan = dataManager.create(Loan.class);
        loan.setLoanCode("LOAN-TEST-" + uniqueSuffix);
        loan.setMember(member);
        loan.setIssuedBy(adminUser);
        loan.setLoanDate(OffsetDateTime.now());
        loan = dataManager.save(loan);

        LoanItem loanItem = dataManager.create(LoanItem.class);
        loanItem.setLoan(loan);
        loanItem.setBookCopy(copy);
        loanItem.setDueDate(LocalDate.now().plusDays(14));
        loanItem.setStatus(LoanItemStatus.BORROWED.getId());
        loanItem = dataManager.save(loanItem);

        // 6. Tạo Fine
        Fine fine = dataManager.create(Fine.class);
        fine.setMember(member);
        fine.setLoanItem(loanItem);
        fine.setFineType(FineType.OVERDUE.getId());
        fine.setAmount(BigDecimal.valueOf(15000));
        fine.setPaidAmount(BigDecimal.ZERO);
        fine.setStatus(FineStatus.UNPAID.getId());
        fine.setCreatedBy(adminUser);
        dataManager.save(fine);

        // Kiểm tra thống kê dữ liệu liên quan
        UserService.UserRelatedSummary summary = userService.inspectUserRelatedData(user.getId());
        assertThat(summary).isNotNull();
        assertThat(summary.hasReaderData()).isTrue();
        assertThat(summary.borrowedBooksCount()).isEqualTo(1);
        assertThat(summary.totalLoansCount()).isEqualTo(1);
        assertThat(summary.totalFinesCount()).isEqualTo(1);

        // Thực hiện xóa user cùng toàn bộ dữ liệu liên đới
        userService.deleteUserAndAllRelatedData(user.getId());

        // Kiểm tra sau khi xóa:
        // User và Member đã biến mất
        assertThat(dataManager.load(User.class).id(user.getId()).optional()).isEmpty();
        assertThat(dataManager.load(Member.class).id(member.getId()).optional()).isEmpty();

        // Loan và Fine của member đã bị xóa
        List<Loan> remainingLoans = dataManager.load(Loan.class)
                .query("select l from Loan l where l.id = :loanId")
                .parameter("loanId", loan.getId())
                .list();
        assertThat(remainingLoans).isEmpty();

        // Bản sao sách đã được giải phóng về AVAILABLE
        BookCopy reloadedCopy = dataManager.load(BookCopy.class).id(copy.getId()).one();
        assertThat(reloadedCopy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE.getId());

        // Dọn dẹp bản sao test
        dataManager.remove(reloadedCopy);
    }
}
