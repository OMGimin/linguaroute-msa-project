package com.lecture.user.service;

import com.lecture.user.entity.Term;
import com.lecture.user.repository.TermRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermServiceTest {

    @Mock
    private TermRepository termRepository;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-10T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void 약관_종류별_최신_버전만_활성_약관으로_반환한다() {
        Term oldTerm = term(1L, "SERVICE_TERMS", "1.0", LocalDateTime.of(2026, 1, 1, 0, 0));
        Term latestTerm = term(2L, "SERVICE_TERMS", "2.0", LocalDateTime.of(2026, 8, 1, 0, 0));
        Term privacyTerm = term(3L, "PRIVACY_COLLECTION", "1.0", LocalDateTime.of(2026, 8, 1, 0, 0));
        when(termRepository.findByEffectiveAtLessThanEqualOrderByTypeAscEffectiveAtDesc(any()))
                .thenReturn(List.of(privacyTerm, latestTerm, oldTerm));

        TermService termService = new TermService(termRepository, clock);

        assertThat(termService.findActiveTerms())
                .extracting(Term::getId)
                .containsExactly(3L, 2L);
    }

    private Term term(Long id, String type, String version, LocalDateTime effectiveAt) {
        return Term.builder()
                .id(id)
                .type(type)
                .version(version)
                .content("content")
                .required(true)
                .effectiveAt(effectiveAt)
                .build();
    }
}
