package com.lecture.enrollment.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [추가]
 * 필수 차시 완료 개수를 기준으로 진도율을 계산한다.
 *
 * 진도율 = 완료한 필수 차시 수 / 전체 필수 차시 수 × 100
 */
@Component
public class ProgressCalculator {

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    /**
     * 필수 차시 진도율을 계산한다.
     *
     * 예:
     * 완료 3개 / 전체 5개 = 60.00
     */
    public BigDecimal calculate(
            long completedRequiredLessons,
            long totalRequiredLessons
    ) {
        validateLessonCounts(
                completedRequiredLessons,
                totalRequiredLessons
        );

        // 필수 차시가 없으면 0으로 처리한다.
        if (totalRequiredLessons == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return BigDecimal
                .valueOf(completedRequiredLessons)
                .multiply(ONE_HUNDRED)
                .divide(
                        BigDecimal.valueOf(totalRequiredLessons),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    /**
     * 모든 필수 차시가 완료되었는지 확인한다.
     *
     * 필수 차시가 0개인 강의는 자동 완료 처리하지 않는다.
     */
    public boolean isCompleted(
            long completedRequiredLessons,
            long totalRequiredLessons
    ) {
        validateLessonCounts(
                completedRequiredLessons,
                totalRequiredLessons
        );

        return totalRequiredLessons > 0
                && completedRequiredLessons
                == totalRequiredLessons;
    }

    private void validateLessonCounts(
            long completedRequiredLessons,
            long totalRequiredLessons
    ) {
        if (completedRequiredLessons < 0) {
            throw new IllegalArgumentException(
                    "완료한 필수 차시 수는 0 이상이어야 합니다."
            );
        }

        if (totalRequiredLessons < 0) {
            throw new IllegalArgumentException(
                    "전체 필수 차시 수는 0 이상이어야 합니다."
            );
        }

        if (completedRequiredLessons > totalRequiredLessons) {
            throw new IllegalArgumentException(
                    "완료한 필수 차시 수는 전체 필수 차시 수보다 클 수 없습니다."
            );
        }
    }
}