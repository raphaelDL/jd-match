package com.rafa.jdmatch.analysis;

import com.rafa.jdmatch.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
        AnalysisOutcome outcome = service.analyze(request.jdText(), request.resumeText());
        URI location = uriBuilder.path("/analyses/{id}").buildAndExpand(outcome.result().id()).toUri();
        // 201 for a freshly produced analysis; 200 when an identical request was
        // already analyzed and we're returning the cached result.
        return outcome.created()
                ? ResponseEntity.created(location).body(outcome.result())
                : ResponseEntity.ok().location(location).body(outcome.result());
    }

    @GetMapping
    public PageResponse<AnalysisSummary> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public AnalysisResult get(@PathVariable UUID id) {
        return service.get(id);
    }
}
