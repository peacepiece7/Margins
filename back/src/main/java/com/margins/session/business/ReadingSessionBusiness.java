package com.margins.session.business;

import com.margins.session.dto.CreateReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionResponse;
import org.springframework.stereotype.Component;

@Component
public class ReadingSessionBusiness {

    public CreateReadingSessionResponse create(CreateReadingSessionRequest request) {
        return CreateReadingSessionResponse.builder()
            .sessionId(1L)
            .bookId(request.getBookId())
            .title(request.getTitle())
            .status("active")
            .build();
    }
}
