package com.lecture.user.service;

import com.lecture.user.dto.TermDto;
import com.lecture.user.entity.Term;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermService {

    private final TermRepository termRepository;
    private final Clock clock;

    public List<TermDto.Response> getActiveTerms() {
        return findActiveTerms().stream()
                .map(TermDto.Response::from)
                .toList();
    }

    public List<Term> findActiveTerms() {
        List<Term> effectiveTerms = termRepository
                .findByEffectiveAtLessThanEqualOrderByTypeAscEffectiveAtDesc(LocalDateTime.now(clock));

        Map<String, Term> latestByType = new LinkedHashMap<>();
        effectiveTerms.forEach(term -> latestByType.putIfAbsent(term.getType(), term));
        return List.copyOf(latestByType.values());
    }

    public List<Term> validateAgreementIds(List<Long> agreementIds, boolean requireAllMandatory) {
        List<Term> activeTerms = findActiveTerms();
        Set<Long> requestedIds = new HashSet<>(agreementIds);
        Set<Long> activeIds = activeTerms.stream()
                .map(Term::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (!activeIds.containsAll(requestedIds)) {
            throw new ApiException(ErrorCode.INVALID_AGREEMENT);
        }

        if (requireAllMandatory) {
            boolean requiredMissing = activeTerms.stream()
                    .filter(Term::isRequired)
                    .map(Term::getId)
                    .anyMatch(id -> !requestedIds.contains(id));
            if (requiredMissing) {
                throw new ApiException(ErrorCode.REQUIRED_AGREEMENT_MISSING);
            }
        }

        return activeTerms.stream()
                .filter(term -> requestedIds.contains(term.getId()))
                .toList();
    }
}
