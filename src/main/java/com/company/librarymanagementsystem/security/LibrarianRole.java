package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "Librarian Role", code = LibrarianRole.CODE)
public interface LibrarianRole {

    String CODE = "librarian-role";

    @EntityPolicy(entityClass = Book.class, actions = { EntityPolicyAction.READ })
    @EntityAttributePolicy(entityClass = Book.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = BookCopy.class, actions = { EntityPolicyAction.READ, EntityPolicyAction.UPDATE })
    @EntityAttributePolicy(entityClass = BookCopy.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Location.class, actions = { EntityPolicyAction.READ })
    @EntityAttributePolicy(entityClass = Location.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Author.class, actions = { EntityPolicyAction.READ })
    @EntityAttributePolicy(entityClass = Author.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Category.class, actions = { EntityPolicyAction.READ })
    @EntityAttributePolicy(entityClass = Category.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Publisher.class, actions = { EntityPolicyAction.READ })
    @EntityAttributePolicy(entityClass = Publisher.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Member.class, actions = {
            EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE })
    @EntityAttributePolicy(entityClass = Member.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = MemberType.class, actions = { EntityPolicyAction.READ })
    @EntityAttributePolicy(entityClass = MemberType.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = User.class, actions = { EntityPolicyAction.READ })
    @EntityAttributePolicy(entityClass = User.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Loan.class, actions = { EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE })
    @EntityAttributePolicy(entityClass = Loan.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = LoanItem.class, actions = { EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE })
    @EntityAttributePolicy(entityClass = LoanItem.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Renewal.class, actions = { EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE })
    @EntityAttributePolicy(entityClass = Renewal.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Fine.class, actions = { EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE })
    @EntityAttributePolicy(entityClass = Fine.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Payment.class, actions = { EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE })
    @EntityAttributePolicy(entityClass = Payment.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Reservation.class, actions = { EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE })
    @EntityAttributePolicy(entityClass = Reservation.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @MenuPolicy(menuIds = { "OpacCatalogView", "LibrarianShiftDashboardView", "librarianManagement", "Loan.list", "Reservation.list", "Fine.list", "Member.list" })
    @ViewPolicy(viewIds = { "OpacCatalogView", "LibrarianShiftDashboardView", "Loan.list", "LoanCreate.view", "Loan.detail", "Reservation.list", "Fine.list", "Member.list", "LibraryMember.detail", "Member.detail" })
    void librarianPermissions();
}
