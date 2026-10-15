package com.eventflow.service;

import com.eventflow.dto.*;
import com.eventflow.entity.enums.PollStatus;
import com.eventflow.entity.enums.QuestionStatus;
import java.util.List;

public interface LiveEngagementService {

    List<QuestionDto> getQuestions(Long eventId, String currentUsername);

    QuestionDto submitQuestion(Long eventId, String currentUsername, QuestionSubmitRequest request);

    QuestionDto toggleUpvote(Long questionId, String currentUsername);

    QuestionDto updateQuestionStatus(Long questionId, String currentUsername, QuestionStatus status);

    List<PollDto> getPolls(Long eventId, String currentUsername);

    PollDto createPoll(Long eventId, String currentUsername, CreatePollRequest request);

    PollDto voteInPoll(Long pollId, String currentUsername, PollVoteRequest request);

    PollDto updatePollStatus(Long pollId, String currentUsername, PollStatus status);
}
