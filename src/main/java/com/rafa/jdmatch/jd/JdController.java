package com.rafa.jdmatch.jd;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jds")
public class JdController {

    private static final int DEFAULT_TOP_K = 5;

    private final JdService jdService;
    private final JdSimilarityService similarityService;

    public JdController(JdService jdService, JdSimilarityService similarityService) {
        this.jdService = jdService;
        this.similarityService = similarityService;
    }

    @PostMapping("/similar")
    public List<SimilarJd> findSimilar(@Valid @RequestBody SimilarJdRequest request) {
        int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();
        return similarityService.findSimilar(request.jdText(), topK);
    }

    /** Backfill: re-index every stored JD into the vector store. */
    @PostMapping("/reindex")
    public ReindexResult reindex() {
        return new ReindexResult(jdService.reindexAll());
    }
}
