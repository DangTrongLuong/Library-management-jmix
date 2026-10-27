package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.entity.User;
import io.jmix.securitydata.user.AbstractDatabaseUserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Primary
@Component("UserRepository")
public class DatabaseUserRepository extends AbstractDatabaseUserRepository<User> {

    @Override
    protected Class<User> getUserClass() {
        return User.class;
    }

    @Override
    protected void initSystemUser(final User systemUser) {
        final Collection<GrantedAuthority> authorities = getGrantedAuthoritiesBuilder()
                .addResourceRole(FullAccessRole.CODE)
                .build();
        systemUser.setAuthorities(authorities);
    }

    @Override
    protected void initAnonymousUser(final User anonymousUser) {
    }

    @Override
    protected Collection<? extends GrantedAuthority> createAuthorities(final String username) {
        List<GrantedAuthority> authorities = new ArrayList<>(super.createAuthorities(username));
        GrantedAuthority uiMinimal = roleGrantedAuthorityUtils.createResourceRoleGrantedAuthority(UiMinimalRole.CODE);
        if (!authorities.contains(uiMinimal)) {
            authorities.add(uiMinimal);
        }

        try {
            User user = dataManager.load(User.class)
                    .query("select u from User u where u.username = :username")
                    .parameter("username", username)
                    .fetchPlan(builder -> builder
                            .add("role", rolePlan -> rolePlan.addFetchPlan(io.jmix.core.FetchPlan.BASE).add("permissions", io.jmix.core.FetchPlan.BASE))
                            .add("permissions", io.jmix.core.FetchPlan.BASE))
                    .optional()
                    .orElse(null);

            if (user != null) {
                boolean isAdmin = "admin".equalsIgnoreCase(username)
                        || (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getCode()));

                if (isAdmin) {
                    addAuthorityIfNotPresent(authorities, FullAccessRole.CODE);
                    // Admin tự động có toàn bộ 43 quyền hạn
                    for (String perm : Permissions.ALL_PERMISSIONS) {
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(perm));
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERM_" + perm));
                    }
                } else {
                    // 1. Phân quyền vai trò Jmix chính
                    if (user.getRole() != null) {
                        String roleCode = user.getRole().getCode();
                        if ("CATALOGER".equalsIgnoreCase(roleCode)) {
                            addAuthorityIfNotPresent(authorities, CatalogerRole.CODE);
                        } else if ("LIBRARIAN".equalsIgnoreCase(roleCode)) {
                            addAuthorityIfNotPresent(authorities, LibrarianRole.CODE);
                        } else if ("MANAGER".equalsIgnoreCase(roleCode)) {
                            addAuthorityIfNotPresent(authorities, ManagerRole.CODE);
                        } else if ("READER".equalsIgnoreCase(roleCode)) {
                            addAuthorityIfNotPresent(authorities, ReaderRole.CODE);
                        }
                    }

                    // 2. Thu thập toàn bộ quyền chi tiết (từ Role + từ cấu hình riêng của User)
                    java.util.Set<String> permCodes = new java.util.HashSet<>();
                    if (user.getRole() != null && user.getRole().getPermissions() != null) {
                        for (com.company.librarymanagementsystem.entity.Permission p : user.getRole().getPermissions()) {
                            if (p.getCode() != null) {
                                permCodes.add(p.getCode().toUpperCase());
                            }
                        }
                    }
                    if (user.getPermissions() != null) {
                        for (com.company.librarymanagementsystem.entity.Permission p : user.getPermissions()) {
                            if (p.getCode() != null) {
                                permCodes.add(p.getCode().toUpperCase());
                            }
                        }
                    }

                    // Nạp từng mã quyền vào Spring Security authorities
                    for (String code : permCodes) {
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(code));
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERM_" + code));
                    }
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(DatabaseUserRepository.class)
                    .error("Lỗi khi nạp quyền người dùng {}: {}", username, e.getMessage(), e);
        }

        return authorities;
    }

    private void addAuthorityIfNotPresent(List<GrantedAuthority> authorities, String roleCode) {
        GrantedAuthority auth = roleGrantedAuthorityUtils.createResourceRoleGrantedAuthority(roleCode);
        if (!authorities.contains(auth)) {
            authorities.add(auth);
        }
    }
}