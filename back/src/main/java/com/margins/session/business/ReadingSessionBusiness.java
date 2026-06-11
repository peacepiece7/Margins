package com.margins.session.business;

import com.margins.session.dto.CreateReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionResponse;
import com.margins.session.mapper.ReadingSessionMapper;
import com.margins.session.model.ReadingSessionRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadingSessionBusiness {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String ACTIVE_STATUS = "active";

    private final ReadingSessionMapper readingSessionMapper;

    public CreateReadingSessionResponse create(CreateReadingSessionRequest request) {
        ReadingSessionRecord record = ReadingSessionRecord.builder()
            .userId(DEFAULT_USER_ID)
            .bookId(request.getBookId())
            .title(request.getTitle())
            .status(ACTIVE_STATUS)
            .testData(true)
            .build();

        readingSessionMapper.insert(record);

        return CreateReadingSessionResponse.builder()
            .sessionId(record.getId())
            .bookId(record.getBookId())
            .title(record.getTitle())
            .status(record.getStatus())
            .build();
    }
}
