package com.eventflow.service;

import com.eventflow.dto.CreateRefundRequest;
import com.eventflow.dto.RefundRequestResponse;
import com.eventflow.dto.ReviewRefundRequest;

import java.util.List;

public interface RefundService {

    RefundRequestResponse requestRefund(Long userId, CreateRefundRequest request);

    List<RefundRequestResponse> getMyRefundRequests(Long userId);

    List<RefundRequestResponse> getAllRefundRequests();

    List<RefundRequestResponse> getOrganizerRefundRequests(Long organizerId);

    RefundRequestResponse processRefund(Long reviewerUserId, Long refundRequestId, ReviewRefundRequest review);
}
