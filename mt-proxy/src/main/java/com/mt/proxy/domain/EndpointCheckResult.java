package com.mt.proxy.domain;

import lombok.Data;

@Data
public class EndpointCheckResult {
    private boolean passed = false;
    private CheckResult reason;

    public static EndpointCheckResult emptyAuth() {
        EndpointCheckResult result = new EndpointCheckResult();
        result.reason = CheckResult.MISSING_AUTH;
        return result;
    }

    public static EndpointCheckResult invalidJwt() {
        EndpointCheckResult result = new EndpointCheckResult();
        result.reason = CheckResult.INVALID_JWT;
        return result;
    }

    public static EndpointCheckResult parseError() {
        EndpointCheckResult result = new EndpointCheckResult();
        result.reason = CheckResult.PARSE_ERROR;
        return result;
    }

    public static EndpointCheckResult missingPermissionId() {
        EndpointCheckResult result = new EndpointCheckResult();
        result.reason = CheckResult.MISSING_PERMISSION_ID;
        return result;
    }

    public static EndpointCheckResult permissionIdNotFound() {
        EndpointCheckResult result = new EndpointCheckResult();
        result.reason = CheckResult.PERMISSION_ID_NOT_FOUND;
        return result;
    }

    public static EndpointCheckResult notFoundInResource() {
        EndpointCheckResult result = new EndpointCheckResult();
        result.reason = CheckResult.NOT_FOUND_IN_RESOURCE;
        return result;
    }

    public static EndpointCheckResult permissionIdMatch() {
        EndpointCheckResult result = new EndpointCheckResult();
        result.passed = true;
        result.setReason(CheckResult.PERMISSION_ID_MATCH);
        return result;
    }

    public static EndpointCheckResult allowPublic() {
        EndpointCheckResult result = new EndpointCheckResult();
        result.passed = true;
        result.setReason(CheckResult.PUBLIC_ENDPOINT);
        return result;
    }
}
