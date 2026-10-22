package com.kinplatform.platform.projectdoc.web;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.platform.projectdoc.DocumentGenerationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class InvestmentMemoController {

    private static final MediaType TEXT_MARKDOWN =
            MediaType.parseMediaType("text/markdown;charset=UTF-8");

    private final DocumentGenerationService documentGenerationService;
    private final UserRepository userRepository;

    @GetMapping(value = "/{projectId}/investment-memo", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<String> getInvestmentMemo(
            Authentication auth, @PathVariable UUID projectId) {
        UUID userId = AuthenticatedUsers.require(userRepository, auth).getId();
        String markdown = documentGenerationService.generateInvestmentMemo(userId, projectId);
        return ResponseEntity.ok()
                .contentType(TEXT_MARKDOWN)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(markdown);
    }
}
