package com.margins.session.service;

import com.margins.session.business.ReadingSessionBusiness;
import com.margins.session.dto.CreateReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReadingSessionService {

    private final ReadingSessionBusiness readingSessionBusiness;

    public CreateReadingSessionResponse create(CreateReadingSessionRequest request) {
        return readingSessionBusiness.create(request);
    }
}
