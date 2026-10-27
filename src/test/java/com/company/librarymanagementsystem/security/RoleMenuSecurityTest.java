package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.entity.Role;
import com.company.librarymanagementsystem.entity.User;
import com.company.librarymanagementsystem.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.security.UserRepository;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class RoleMenuSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataManager dataManager;

    @Test
    void testManagerRolePolicyIsolation() {
        Method[] methods = ManagerRole.class.getDeclaredMethods();
        assertThat(methods).isNotEmpty();
        Method method = methods[0];

        MenuPolicy menuPolicy = method.getAnnotation(MenuPolicy.class);
        assertThat(menuPolicy).isNotNull();
        List<String> menuIds = Arrays.asList(menuPolicy.menuIds());

        // Manager should NOT have Librarian shift dashboard
        assertThat(menuIds).doesNotContain("LibrarianShiftDashboardView");
        assertThat(menuIds).doesNotContain("librarianShiftDash");

        // Manager should NOT have Cataloger management
        assertThat(menuIds).doesNotContain("catalogManagement");
        assertThat(menuIds).doesNotContain("BookCopy.list");
        assertThat(menuIds).doesNotContain("Location.list");
        assertThat(menuIds).doesNotContain("Author.list");
        assertThat(menuIds).doesNotContain("Category.list");
        assertThat(menuIds).doesNotContain("Publisher.list");

        // Manager should NOT have Reader services
        assertThat(menuIds).doesNotContain("readerServices");

        ViewPolicy viewPolicy = method.getAnnotation(ViewPolicy.class);
        assertThat(viewPolicy).isNotNull();
        List<String> viewIds = Arrays.asList(viewPolicy.viewIds());
        assertThat(viewIds).doesNotContain("LibrarianShiftDashboardView");
        assertThat(viewIds).doesNotContain("Role.list");
    }

    @Test
    void testLibrarianRolePolicyIsolation() {
        Method[] methods = LibrarianRole.class.getDeclaredMethods();
        assertThat(methods).isNotEmpty();
        Method method = methods[0];

        MenuPolicy menuPolicy = method.getAnnotation(MenuPolicy.class);
        assertThat(menuPolicy).isNotNull();
        List<String> menuIds = Arrays.asList(menuPolicy.menuIds());

        // Librarian should NOT have Manager operations
        assertThat(menuIds).doesNotContain("ManagerDashboardView");
        assertThat(menuIds).doesNotContain("managerOperations");
        assertThat(menuIds).doesNotContain("LibrarySettingView");
        assertThat(menuIds).doesNotContain("FineWaiveApprovalView");
        assertThat(menuIds).doesNotContain("ExceptionHandlingView");
        assertThat(menuIds).doesNotContain("AuditLogListView");
        assertThat(menuIds).doesNotContain("MemberType.list");

        // Librarian should NOT have Cataloger or Admin
        assertThat(menuIds).doesNotContain("catalogManagement");
        assertThat(menuIds).doesNotContain("administration");
        assertThat(menuIds).doesNotContain("readerServices");
    }

    @Test
    void testCatalogerRolePolicyIsolation() {
        Method[] methods = CatalogerRole.class.getDeclaredMethods();
        assertThat(methods).isNotEmpty();
        Method method = methods[0];

        MenuPolicy menuPolicy = method.getAnnotation(MenuPolicy.class);
        assertThat(menuPolicy).isNotNull();
        List<String> menuIds = Arrays.asList(menuPolicy.menuIds());

        // Cataloger should NOT have Manager or Librarian or Reader
        assertThat(menuIds).doesNotContain("ManagerDashboardView");
        assertThat(menuIds).doesNotContain("LibrarianShiftDashboardView");
        assertThat(menuIds).doesNotContain("managerOperations");
        assertThat(menuIds).doesNotContain("librarianManagement");
        assertThat(menuIds).doesNotContain("readerServices");
        assertThat(menuIds).doesNotContain("administration");
    }

    @Test
    void testReaderRolePolicyIsolation() {
        Method[] methods = ReaderRole.class.getDeclaredMethods();
        assertThat(methods).isNotEmpty();
        Method method = methods[0];

        MenuPolicy menuPolicy = method.getAnnotation(MenuPolicy.class);
        assertThat(menuPolicy).isNotNull();
        List<String> menuIds = Arrays.asList(menuPolicy.menuIds());

        // Reader should ONLY have readerServices and OpacCatalogView
        assertThat(menuIds).doesNotContain("ManagerDashboardView");
        assertThat(menuIds).doesNotContain("LibrarianShiftDashboardView");
        assertThat(menuIds).doesNotContain("managerOperations");
        assertThat(menuIds).doesNotContain("librarianManagement");
        assertThat(menuIds).doesNotContain("catalogManagement");
        assertThat(menuIds).doesNotContain("administration");
    }

    @Test
    void testUserAuthoritiesIsolationByRole() {
        // Find existing roles from DB
        List<Role> roles = dataManager.load(Role.class).all().list();
        Role librarianRole = roles.stream().filter(r -> "LIBRARIAN".equalsIgnoreCase(r.getCode())).findFirst().orElse(null);
        Role catalogerRole = roles.stream().filter(r -> "CATALOGER".equalsIgnoreCase(r.getCode())).findFirst().orElse(null);

        if (librarianRole != null) {
            User testLibrarian = dataManager.create(User.class);
            testLibrarian.setUsername("test-lib-" + System.currentTimeMillis());
            testLibrarian.setPassword("password");
            testLibrarian.setRole(librarianRole);
            User saved = dataManager.save(testLibrarian);

            try {
                UserDetails details = userRepository.loadUserByUsername(saved.getUsername());
                Set<String> authNames = details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                // Must have librarian-role
                assertThat(authNames).contains("ROLE_" + LibrarianRole.CODE);
                // MUST NOT have manager-role, cataloger-role, reader-role, or system-full-access
                assertThat(authNames).doesNotContain("ROLE_" + ManagerRole.CODE);
                assertThat(authNames).doesNotContain("ROLE_" + CatalogerRole.CODE);
                assertThat(authNames).doesNotContain("ROLE_" + ReaderRole.CODE);
                assertThat(authNames).doesNotContain("ROLE_" + FullAccessRole.CODE);
            } finally {
                dataManager.remove(saved);
            }
        }

        if (catalogerRole != null) {
            User testCataloger = dataManager.create(User.class);
            testCataloger.setUsername("test-cat-" + System.currentTimeMillis());
            testCataloger.setPassword("password");
            testCataloger.setRole(catalogerRole);
            User saved = dataManager.save(testCataloger);

            try {
                UserDetails details = userRepository.loadUserByUsername(saved.getUsername());
                Set<String> authNames = details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                // Must have cataloger-role
                assertThat(authNames).contains("ROLE_" + CatalogerRole.CODE);
                // MUST NOT have manager-role, librarian-role, reader-role, or system-full-access
                assertThat(authNames).doesNotContain("ROLE_" + ManagerRole.CODE);
                assertThat(authNames).doesNotContain("ROLE_" + LibrarianRole.CODE);
                assertThat(authNames).doesNotContain("ROLE_" + ReaderRole.CODE);
                assertThat(authNames).doesNotContain("ROLE_" + FullAccessRole.CODE);
            } finally {
                dataManager.remove(saved);
            }
        }
    }
}
