package com.eventflow.service;

import com.eventflow.dto.AiCopyRequest;
import com.eventflow.dto.AiCopyResponse;

public interface AiCopywriterService {
    AiCopyResponse generateEventCopy(AiCopyRequest request, Long currentUserId);
}
