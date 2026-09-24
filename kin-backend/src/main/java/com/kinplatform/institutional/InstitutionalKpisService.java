package com.kinplatform.institutional;

import com.kinplatform.billing.dashboard.BillingDashboardService;
import com.kinplatform.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstitutionalKpisService {

    private final BranchRepository branchRepository;
    private final OrganizationMemberRepository memberRepository;
    private final BillingDashboardService billingDashboardService;

    public InstitutionalKpisResponse compute() {
        UUID organizationId = TenantContext.get();
        long branchCount = branchRepository.findByOrganizationIdOrderByNameAsc(organizationId).size();
        var members = memberRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        long activeMembers = members.stream()
                .filter(m -> m.getStatus() == OrganizationMember.MemberStatus.ACTIVE)
                .count();
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (OrganizationMember member : members) {
            byRole.merge(member.getRole().name(), 1L, Long::sum);
        }
        return new InstitutionalKpisResponse(
                branchCount, members.size(), activeMembers, byRole,
                billingDashboardService.kpis(null, null));
    }
}
