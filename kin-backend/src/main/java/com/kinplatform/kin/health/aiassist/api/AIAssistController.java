package com.kinplatform.kin.health.aiassist.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.aiassist.domain.AIAssistRequest;
import com.kinplatform.kin.health.aiassist.service.AIAssistService;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health/aiassist")
public class AIAssistController {

    private final AIAssistService assistService;
    private final UserRepository userRepository;

    public AIAssistController(AIAssistService assistService, UserRepository userRepository) {
        this.assistService = assistService;
        this.userRepository = userRepository;
    }

    @PostMapping("/patients/{patientId}/summary")
    public ResponseEntity<AIAssistRequest> generateSummary(
            Authentication authentication, @PathVariable UUID patientId) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        AIAssistRequest request = assistService.generateSummary(user.getId(), patientId);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/patients/{patientId}/consultation-prep")
    public ResponseEntity<AIAssistRequest> prepareConsultation(
            Authentication authentication, @PathVariable UUID patientId) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        AIAssistRequest request = assistService.prepareConsultation(user.getId(), patientId);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/patients/{patientId}/draft-message")
    public ResponseEntity<AIAssistRequest> draftMessage(
            Authentication authentication,
            @PathVariable UUID patientId,
            @RequestBody @Valid MessageDraftRequest request) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        AIAssistRequest response = assistService.draftMessage(user.getId(), patientId, request.recommendation());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/differential-explain")
    public ResponseEntity<AIAssistRequest> explainDifferential(
            Authentication authentication, @RequestBody ExplainRequest request) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        AIAssistRequest response = assistService.explainDifferential(user.getId(), request.differentialResult());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/history")
    public ResponseEntity<List<AIAssistRequest>> history(Authentication authentication, @PathVariable UUID patientId) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        return ResponseEntity.ok(assistService.getHistory(user.getId(), patientId));
    }

    @GetMapping("/my/history")
    public ResponseEntity<List<AIAssistRequest>> myHistory(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        return ResponseEntity.ok(assistService.getMyHistory(user.getId()));
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(Authentication authentication, @PathVariable UUID id) {
        AuthenticatedUsers.require(userRepository, authentication);
        return ResponseEntity.noContent().build();
    }

    public record MessageDraftRequest(String recommendation) {}

    public record ExplainRequest(TriageResult differentialResult) {}
}
