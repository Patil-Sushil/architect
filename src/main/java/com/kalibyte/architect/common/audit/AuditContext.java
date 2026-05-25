package com.kalibyte.architect.common.audit;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuditContext {
    private UUID entityId;
    private String entityName;
    private String oldValue;
    private String newValue;
}