package com.placement.portal.recruitment;

import com.placement.portal.company.CompanyDto;
import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface IRecruiterEvaluationService {
    List<CompanyDto> getAuthorizedCompanies(Principal principal);
    Map<String, Object> selectActiveCompany(String companyId, Principal principal);
    ResumeEvaluationDto recordEvaluation(ResumeEvaluationDto dto, Principal principal);
    List<ResumeEvaluationDto> getEvaluationsByApplication(String applicationId);
    List<RecruiterRoleStatDto> getRecruiterRoleStats(String companyId, Principal principal);
}
