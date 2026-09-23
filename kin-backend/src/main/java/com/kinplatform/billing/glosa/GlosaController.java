package com.kinplatform.billing.glosa;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/billing/glosas")
@RequiredArgsConstructor
public class GlosaController {

    private final GlosaService glosaService;

    @PostMapping("/import/{contractId}")
    public ResponseEntity<List<Glosa>> importGlosas(@PathVariable UUID contractId,
                                                    @RequestBody String content) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(glosaService.importFromFile(contractId, content));
    }

    @GetMapping
    public ResponseEntity<List<Glosa>> list(@RequestParam(required = false) Glosa.GlosaStatus status) {
        return ResponseEntity.ok(status == null ? glosaService.findAll() : glosaService.findByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Glosa> get(@PathVariable UUID id) {
        return ResponseEntity.ok(glosaService.get(id));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<Glosa> analyze(@PathVariable UUID id) {
        return ResponseEntity.ok(glosaService.analyze(id));
    }

    @PostMapping("/{id}/appeal")
    public ResponseEntity<Glosa> submitAppeal(@PathVariable UUID id, @RequestBody AppealRequest request) {
        return ResponseEntity.ok(glosaService.submitAppeal(id, request.arguments()));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Glosa> resolve(@PathVariable UUID id, @RequestBody ResolveRequest request) {
        return ResponseEntity.ok(glosaService.resolve(
                id, Glosa.GlosaStatus.valueOf(request.status()), request.resolvedValue()));
    }

    @PostMapping("/{id}/assign/{userId}")
    public ResponseEntity<Glosa> assign(@PathVariable UUID id, @PathVariable UUID userId) {
        return ResponseEntity.ok(glosaService.assign(id, userId));
    }

    @PostMapping("/{id}/match")
    public ResponseEntity<Glosa> match(@PathVariable UUID id) {
        return ResponseEntity.ok(glosaService.matchToRips(id));
    }

    public record AppealRequest(String arguments) {
    }

    public record ResolveRequest(String status, BigDecimal resolvedValue) {
    }
}
