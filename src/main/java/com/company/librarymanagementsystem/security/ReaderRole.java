package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "Reader Role", code = ReaderRole.CODE)
public interface ReaderRole {

    String CODE = "reader-role";

    @EntityPolicy(entityClass = Book.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Book.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = BookCopy.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = BookCopy.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Location.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Location.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Author.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Author.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Category.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Category.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Publisher.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Publisher.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Reservation.class, actions = {EntityPolicyAction.CREATE, EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = Reservation.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Member.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = Member.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = MemberType.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = MemberType.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Loan.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Loan.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = LoanItem.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = LoanItem.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Renewal.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.CREATE})
    @EntityAttributePolicy(entityClass = Renewal.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)

    @EntityPolicy(entityClass = Fine.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Fine.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @EntityPolicy(entityClass = Payment.class, actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Payment.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)

    @MenuPolicy(menuIds = {"readerServices", "OpacCatalogView", "ReaderProfileView", "ReaderCurrentLoansView", "ReaderLoanHistoryView", "ReaderReservationsView", "ReaderFinesView"})
    @ViewPolicy(viewIds = {"OpacCatalogView", "ReaderProfileView", "ReaderCurrentLoansView", "ReaderLoanHistoryView", "ReaderReservationsView", "ReaderFinesView"})
    void readerPermissions();
}
