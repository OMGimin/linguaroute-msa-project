package com.lecture.user.service;

import com.lecture.user.dto.UserDto;
import com.lecture.user.entity.Term;
import com.lecture.user.entity.User;
import com.lecture.user.entity.UserAgreement;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.UserAgreementRepository;
import com.lecture.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAgreementService {

    private final UserRepository userRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final TermService termService;
    private final Clock clock;

    @Transactional
    public UserDto.AgreementResponse agree(Long userId, UserDto.AgreementRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }
        List<Term> terms = termService.validateAgreementIds(request.getAgreementIds(), false);
        LocalDateTime agreedAt = LocalDateTime.now(clock);

        List<UserAgreement> newAgreements = terms.stream()
                .filter(term -> !userAgreementRepository.existsByUserIdAndTermId(userId, term.getId()))
                .map(term -> UserAgreement.builder()
                        .user(user)
                        .term(term)
                        .agreed(true)
                        .agreedAt(agreedAt)
                        .build())
                .toList();
        userAgreementRepository.saveAll(newAgreements);

        return new UserDto.AgreementResponse(terms.stream().map(Term::getId).toList());
    }
}
