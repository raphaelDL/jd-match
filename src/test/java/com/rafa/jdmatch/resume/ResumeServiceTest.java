package com.rafa.jdmatch.resume;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeServiceTest {

    private final ResumeRepository repository = mock(ResumeRepository.class);
    private final ResumeService service = new ResumeService(repository);

    @Test
    void missStoresNewResume() {
        when(repository.findByResumeHash(any())).thenReturn(Optional.empty());
        when(repository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        StoredResume result = service.getOrStore("my resume");

        assertThat(result.id()).isNotNull();
        verify(repository).save(any(Resume.class));
    }

    @Test
    void hitReturnsExistingWithoutSaving() {
        UUID id = UUID.randomUUID();
        Resume stored = new Resume(id, "hash", "my resume", Instant.now());
        when(repository.findByResumeHash(any())).thenReturn(Optional.of(stored));

        StoredResume result = service.getOrStore("my resume");

        assertThat(result.id()).isEqualTo(id);
        verify(repository, never()).save(any());
    }
}
