package com.lecture.user.dto;

import com.lecture.user.entity.Term;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

public class TermDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String type;
        private String version;
        private String content;
        private boolean required;
        private LocalDateTime effectiveAt;

        public static Response from(Term term) {
            return new Response(
                    term.getId(),
                    term.getType(),
                    term.getVersion(),
                    term.getContent(),
                    term.isRequired(),
                    term.getEffectiveAt()
            );
        }
    }
}
