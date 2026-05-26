package com.rafa.jdmatch.analysis;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/analyses")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AnalysisResult> create(@Valid @RequestBody AnalyzeRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        AnalysisResult result = service.analyze(request.jdText(), request.resumeText());
        URI location = uriBuilder.path("/analyses/{id}").buildAndExpand(result.id()).toUri();
        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/{id}")
    public AnalysisResult get(@PathVariable UUID id) {
        return service.get(id);
    }
}
