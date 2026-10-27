package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.AuditLog;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final DataManager dataManager;
    private final CurrentAuthentication currentAuthentication;

    public AuditLogService(DataManager dataManager, CurrentAuthentication currentAuthentication) {
        this.dataManager = dataManager;
        this.currentAuthentication = currentAuthentication;
    }

    public void log(String action, String entityName, String entityId, String details) {
        try {
            String username = "SYSTEM";
            if (currentAuthentication != null && currentAuthentication.isSet()) {
                username = currentAuthentication.getUser().getUsername();
            }

            AuditLog auditLog = dataManager.create(AuditLog.class);
            auditLog.setAction(action);
            auditLog.setEntityName(entityName);
            auditLog.setEntityId(entityId);
            auditLog.setUsername(username);
            auditLog.setDetails(details);
            auditLog.setCreatedAt(OffsetDateTime.now());

            dataManager.save(auditLog);
            log.info("Audit: [{}] {} id={} by {}: {}", action, entityName, entityId, username, details);
        } catch (Exception e) {
            log.warn("Lỗi khi ghi audit log: {}", e.getMessage());
        }
    }
}
