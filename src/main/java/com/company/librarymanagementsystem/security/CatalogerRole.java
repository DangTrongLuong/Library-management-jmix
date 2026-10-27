package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "Cataloger Role", code = CatalogerRole.CODE)
public interface CatalogerRole {

    String CODE = "cataloger-role";

    @MenuPolicy(menuIds = {"OpacCatalogView", "catalogManagement", "Book.list", "BookCopy.list", "Location.list", "Author.list", "Category.list", "Publisher.list"})
    @ViewPolicy(viewIds = {
            "OpacCatalogView",
            "Book.list", "Book.detail",
            "BookCopy.list", "BookCopy.detail",
            "BulkCopy.create", "BulkCopy.relocate", "Barcode.print",
            "Location.list", "Location.detail",
            "Author.list", "Author.detail",
            "Category.list", "Category.detail",
            "Publisher.list", "Publisher.detail"
    })
    @EntityPolicy(entityClass = Book.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = Book.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = BookCopy.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = BookCopy.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Location.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = Location.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Author.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = Author.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Category.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = Category.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Publisher.class, actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = Publisher.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    void catalogerPermissions();
}

