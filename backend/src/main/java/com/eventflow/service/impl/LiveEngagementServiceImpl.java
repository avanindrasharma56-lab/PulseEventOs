package com.eventflow.service.impl;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.PollStatus;
import com.eventflow.entity.enums.QuestionStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.LiveEngagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LiveEngagementServiceImpl implements LiveEngagementService {

    private final SessionQuestionRepository sessionQuestionRepository;
    private final QuestionUpvoteRepository questionUpvoteRepository;
    private final LivePollRepository livePollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LiveEngagementServiceImpl(
            SessionQuestionRepository sessionQuestionRepository,
            QuestionUpvoteRepository questionUpvoteRepository,
            LivePollRepository livePollRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.sessionQuestionRepository = sessionQuestionRepository;
        this.questionUpvoteRepository = questionUpvoteRepository;
        this.livePollRepository = livePollRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestions(Long eventId, String currentUsername) {
        User currentUser = resolveUser(currentUsername);
        List<SessionQuestion> questions = sessionQuestionRepository
                .findByEventIdAndStatusNotOrderByUpvotesDescCreatedAtDesc(eventId, QuestionStatus.DISMISSED);

        return questions.stream()
                .map(q -> mapToQuestionDto(q, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    public QuestionDto submitQuestion(Long eventId, String currentUsername, QuestionSubmitRequest request) {
        if (request == null || request.getQuestionText() == null || request.getQuestionText().trim().isBlank()) {
            throw new BadRequestException("Question text cannot be blank.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUsername));

        String authorName = request.isAnonymous() ? "Anonymous Attendee" :
                (user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail());

        SessionQuestion question = new SessionQuestion(
                event,
                request.getSessionId(),
                user,
                authorName,
                request.getQuestionText().trim(),
                request.isAnonymous()
        );

        SessionQuestion saved = sessionQuestionRepository.save(question);
        QuestionDto dto = mapToQuestionDto(saved, user);

        broadcastWebSocket("/topic/events/" + eventId + "/qa", dto);
        return dto;
    }

    @Override
    public QuestionDto toggleUpvote(Long questionId, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUsername));

        SessionQuestion question = sessionQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        boolean alreadyUpvoted = questionUpvoteRepository.existsByQuestionIdAndUserId(questionId, user.getId());

        if (alreadyUpvoted) {
            questionUpvoteRepository.deleteByQuestionIdAndUserId(questionId, user.getId());
            question.setUpvotes(Math.max(0, question.getUpvotes() - 1));
        } else {
            questionUpvoteRepository.save(new QuestionUpvote(questionId, user.getId()));
            question.setUpvotes(question.getUpvotes() + 1);
        }

        SessionQuestion saved = sessionQuestionRepository.save(question);
        QuestionDto dto = mapToQuestionDto(saved, user);

        broadcastWebSocket("/topic/events/" + question.getEvent().getId() + "/qa", dto);
        return dto;
    }

    @Override
    public QuestionDto updateQuestionStatus(Long questionId, String currentUsername, QuestionStatus status) {
        SessionQuestion question = sessionQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        verifyOrganizerOrAdmin(question.getEvent(), currentUsername);

        question.setStatus(status != null ? status : QuestionStatus.OPEN);
        SessionQuestion saved = sessionQuestionRepository.save(question);

        User currentUser = resolveUser(currentUsername);
        QuestionDto dto = mapToQuestionDto(saved, currentUser);

        broadcastWebSocket("/topic/events/" + question.getEvent().getId() + "/qa", dto);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PollDto> getPolls(Long eventId, String currentUsername) {
        User currentUser = resolveUser(currentUsername);
        List<LivePoll> polls = livePollRepository.findByEventIdOrderByCreatedAtDesc(eventId);

        return polls.stream()
                .map(p -> mapToPollDto(p, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    public PollDto createPoll(Long eventId, String currentUsername, CreatePollRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isBlank()) {
            throw new BadRequestException("Poll question cannot be empty.");
        }
        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new BadRequestException("A poll requires at least 2 options.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        verifyOrganizerOrAdmin(event, currentUsername);

        LivePoll poll = new LivePoll(event, request.getSessionId(), request.getQuestion().trim());
        List<PollOption> optionEntities = new ArrayList<>();
        int order = 0;
        for (String optText : request.getOptions()) {
            if (optText != null && !optText.trim().isBlank()) {
                optionEntities.add(new PollOption(poll, optText.trim(), order++));
            }
        }

        poll.setOptions(optionEntities);
        LivePoll saved = livePollRepository.save(poll);

        User currentUser = resolveUser(currentUsername);
        PollDto dto = mapToPollDto(saved, currentUser);

        broadcastWebSocket("/topic/events/" + eventId + "/polls", dto);
        return dto;
    }

    @Override
    public PollDto voteInPoll(Long pollId, String currentUsername, PollVoteRequest request) {
        if (request == null || request.getOptionId() == null) {
            throw new BadRequestException("Please select a poll option to vote.");
        }

        LivePoll poll = livePollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));

        if (poll.getStatus() != PollStatus.ACTIVE) {
            throw new BadRequestException("This poll is closed and no longer accepting votes.");
        }

        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUsername));

        if (pollVoteRepository.existsByPollIdAndUserId(pollId, user.getId())) {
            throw new BadRequestException("You have already voted in this poll.");
        }

        PollOption selectedOption = poll.getOptions().stream()
                .filter(o -> o.getId().equals(request.getOptionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("PollOption", "id", request.getOptionId()));

        selectedOption.setVoteCount(selectedOption.getVoteCount() + 1);
        pollOptionRepository.save(selectedOption);

        PollVote vote = new PollVote(poll, selectedOption, user);
        pollVoteRepository.save(vote);

        PollDto dto = mapToPollDto(poll, user);
        broadcastWebSocket("/topic/events/" + poll.getEvent().getId() + "/polls", dto);
        return dto;
    }

    @Override
    public PollDto updatePollStatus(Long pollId, String currentUsername, PollStatus status) {
        LivePoll poll = livePollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));

        verifyOrganizerOrAdmin(poll.getEvent(), currentUsername);

        poll.setStatus(status != null ? status : PollStatus.ACTIVE);
        LivePoll saved = livePollRepository.save(poll);

        User currentUser = resolveUser(currentUsername);
        PollDto dto = mapToPollDto(saved, currentUser);

        broadcastWebSocket("/topic/events/" + poll.getEvent().getId() + "/polls", dto);
        return dto;
    }

    private void verifyOrganizerOrAdmin(Event event, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", username));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOrganizer = event.getOrganizer() != null && event.getOrganizer().getId().equals(user.getId());

        if (!isAdmin && !isOrganizer) {
            throw new AccessDeniedException("Only the event organizer or an administrator may perform this action.");
        }
    }

    private User resolveUser(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }
        return userRepository.findByEmail(username).orElse(null);
    }

    private QuestionDto mapToQuestionDto(SessionQuestion q, User currentUser) {
        QuestionDto dto = new QuestionDto();
        dto.setId(q.getId());
        dto.setEventId(q.getEvent().getId());
        dto.setSessionId(q.getSessionId());
        dto.setAuthorName(q.getAuthorName());
        dto.setQuestionText(q.getQuestionText());
        dto.setUpvotes(q.getUpvotes());
        dto.setAnonymous(q.isAnonymous());
        dto.setStatus(q.getStatus());
        dto.setCreatedAt(q.getCreatedAt());

        if (currentUser != null) {
            dto.setUserHasUpvoted(questionUpvoteRepository.existsByQuestionIdAndUserId(q.getId(), currentUser.getId()));
        } else {
            dto.setUserHasUpvoted(false);
        }
        return dto;
    }

    private PollDto mapToPollDto(LivePoll p, User currentUser) {
        PollDto dto = new PollDto();
        dto.setId(p.getId());
        dto.setEventId(p.getEvent().getId());
        dto.setSessionId(p.getSessionId());
        dto.setQuestion(p.getQuestion());
        dto.setStatus(p.getStatus());
        dto.setCreatedAt(p.getCreatedAt());

        int totalVotes = p.getOptions().stream().mapToInt(PollOption::getVoteCount).sum();
        dto.setTotalVotes(totalVotes);

        List<PollOptionDto> optionDtos = p.getOptions().stream().map(o -> {
            PollOptionDto optDto = new PollOptionDto();
            optDto.setId(o.getId());
            optDto.setOptionText(o.getOptionText());
            optDto.setVoteCount(o.getVoteCount());
            optDto.setDisplayOrder(o.getDisplayOrder());
            double pct = totalVotes > 0 ? (o.getVoteCount() * 100.0) / totalVotes : 0.0;
            optDto.setPercentage(Math.round(pct * 10.0) / 10.0);
            return optDto;
        }).collect(Collectors.toList());

        dto.setOptions(optionDtos);

        if (currentUser != null) {
            pollVoteRepository.findByPollIdAndUserId(p.getId(), currentUser.getId())
                    .ifPresent(v -> dto.setUserVotedOptionId(v.getOption().getId()));
        }

        return dto;
    }

    private void broadcastWebSocket(String destination, Object payload) {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend(destination, payload);
            } catch (Exception e) {
                // Log and gracefully continue
            }
        }
    }
}
