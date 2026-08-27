package com.kinplatform.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnóstico SMTP (solo ADMIN, vía {@code SecurityConfig} para {@code /admin/**}).
 */
@RestController
@RequestMapping("/admin/health/email")
@RequiredArgsConstructor
public class MailDiagnosticController {

    private final MailDiagnosticService mailDiagnosticService;

    @GetMapping("/diagnostic")
    public MailDiagnosticService.DiagnosticResult diagnostic(@RequestParam(value = "to", required = false) String to) {
        return mailDiagnosticService.diagnose(to);
    }
}
