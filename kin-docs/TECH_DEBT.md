# Technical Debt Register — KIN Medical Platform

## Overview
This document tracks known technical debt items that must be addressed before production release or in upcoming sprints.

---

## TD-001: Mockito Mocks - JwtService.generateToken Signature Change

**Priority**: Medium  
**Impact**: Blocks test execution for 14 test methods  
**Effort**: ~2 hours  

### Description
The `JwtService.generateToken()` method signature was changed to include `organizationId` parameter for multi-tenant support:
```java
// Old signature (used in 14 test mocks)
generateToken(UUID userId, String email, String role, String platform)

// New signature
generateToken(UUID userId, String email, String role, String platform, UUID organizationId)
```

### Affected Tests
| Test Class | Methods Affected | Location |
|------------|------------------|----------|
| `AuthServiceImplTest` | 7 methods | `src/test/java/com/kinplatform/auth/AuthServiceImplTest.java` |
| `VerticalAccessTest` | 7 methods | `src/test/java/com/kinplatform/auth/VerticalAccessTest.java` |

### Required Fix
Update all mock stubs from:
```java
when(jwtService.generateToken(any(), anyString(), anyString(), anyString())).thenReturn("token");
```
To:
```java
when(jwtService.generateToken(any(), anyString(), anyString(), anyString(), any())).thenReturn("token");
```

### Acceptance Criteria
- All 14 test methods in `AuthServiceImplTest` and `VerticalAccessTest` pass
- No Mockito `PotentialStubbingProblem` warnings

---

## TD-002: Testcontainers / Docker Dependency for Integration Tests

**Priority**: High  
**Impact**: 32 integration tests skipped/failed due to missing Docker  
**Effort**: Infrastructure setup  

### Description
Multiple integration tests use Testcontainers with PostgreSQL 18 but Docker Desktop is not available in the CI/CD environment.

### Affected Tests
All tests extending `PostgresTestSupport` or using `@SpringBootTest` with database:
- `EnterpriseProjectJpaIntegrationTest`
- `RedisKnowledgeRepositoryTest`
- `ProjectReportJsonbJpaTest`
- `AiUsageJpaIntegrationTest`
- `ProjectQuotaFreeRegressionTest`
- `ChatStreamSecurityAsyncTest`
- `EnterpriseRuntimeCertificationTest`
- `AIAssistIntegrationTest`
- `AuditIntegrationTest`
- `JpaDashboardRepositoryIntegrationTest`
- `JpaDifferentialRepositoryIntegrationTest`
- `DocumentIntegrationTest`
- `DocumentStorageQuotaPortImplTest`
- `FollowUpIntegrationTest`
- `JpaPhysicianRepositoryIntegrationTest`
- `PhysicianPatientRelationshipIntegrationTest`
- `SchedulingIntegrationTest`
- `JpaTelemedicineRepositoryIntegrationTest`
- `JpaTriageRepositoryIntegrationTest`
- `OutboxRelayDisabledIntegrationTest`
- `OutboxRelayIntegrationTest`
- `PricingPlanFeaturesJpaTest`
- `PricingPlanV17BillingPeriodTest`
- `ProjectStructuredInfoJpaTest`
- `JpaTelemedicineRepositoryIntegrationTest`
- `JpaTriageRepositoryIntegrationTest`
- `SecurityConfigCorsTest`

### Required Fix
- Provision Docker-enabled CI/CD runners
- Or migrate to H2 in-memory database for CI tests
- Or use shared PostgreSQL test instance

---

## TD-003: SecurityConfig CORS Configuration Null Pointer

**Priority**: Low  
**Impact**: 2 test failures in `SecurityConfigCorsTest`  
**Effort**: ~30 minutes  

### Description
`CorsConfiguration.getAllowedOrigins()` returns null causing NPE in tests.

---

## TD-004: DocumentStorageQuotaPortImplTest Unnecessary Stubbing

**Priority**: Low  
**Impact**: 4 test warnings in `DocumentStorageQuotaPortImplTest`  
**Effort**: ~15 minutes  

---

## TD-005: AuthControllerTest VerifyEmail NPE

**Priority**: Medium  
**Impact**: 4 test failures in `AuthControllerTest`  
**Effort**: ~30 minutes  

### Description
`VerifyEmailOutcome` is null in controller tests causing NPE.

---

## TD-006: PricingPlanV17BillingPeriodTest ExceptionInInitializerError

**Priority**: Low  
**Impact**: 1 test error  
**Effort**: ~15 minutes  

---

## TD-007: EnterpriseProjectJpaIntegrationTest ExceptionInInitializerError

**Priority**: Low  
**Impact**: 1 test error (related to Testcontainers/Docker)  

---

## Summary
| Category | Count | Priority |
|----------|-------|----------|
| Test Mock Updates | 14 | Medium |
| Infrastructure (Docker) | 32 | High |
| Null Pointer Fixes | 10 | Medium |
| Other | 6 | Low |
| **Total** | **62** |  |

---

*Last Updated: 2026-09-22*  
*Owner: Backend Team*  
*Next Review: Sprint Planning*