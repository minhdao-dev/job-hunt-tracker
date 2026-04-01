package com.jobhunt.tracker.module.offer.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.offer.dto.CreateOfferRequest;
import com.jobhunt.tracker.module.offer.dto.OfferResponse;
import com.jobhunt.tracker.module.offer.dto.UpdateOfferDecisionRequest;
import com.jobhunt.tracker.module.offer.dto.UpdateOfferRequest;
import com.jobhunt.tracker.module.offer.entity.Offer;
import com.jobhunt.tracker.module.offer.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final JobApplicationRepository jobRepository;

    @Override
    @Transactional
    public OfferResponse create(UUID userId, UUID jobId, CreateOfferRequest request) {
        JobApplication job = findJob(userId, jobId);

        if (offerRepository.existsByJobId(jobId)) {
            throw AppException.conflict(
                    "Offer already exists for job: " + jobId
            );
        }

        Offer offer = Offer.builder()
                .job(job)
                .salary(request.salary())
                .currency(request.currency() != null ? request.currency() : "VND")
                .benefits(request.benefits())
                .startDate(request.startDate())
                .expiredAt(request.expiredAt())
                .note(request.note())
                .build();

        offer = offerRepository.save(offer);

        log.info("Offer created for job: {}", jobId);

        return toResponse(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponse get(UUID userId, UUID jobId) {
        return toResponse(findOffer(userId, jobId));
    }

    @Override
    @Transactional
    public OfferResponse update(UUID userId, UUID jobId, UpdateOfferRequest request) {
        Offer offer = findOffer(userId, jobId);

        offer.setSalary(request.salary());
        offer.setCurrency(request.currency() != null ? request.currency() : "VND");
        offer.setBenefits(request.benefits());
        offer.setStartDate(request.startDate());
        offer.setExpiredAt(request.expiredAt());
        offer.setNote(request.note());

        offer = offerRepository.save(offer);

        log.info("Offer updated for job: {}", jobId);

        return toResponse(offer);
    }

    @Override
    @Transactional
    public OfferResponse updateDecision(UUID userId, UUID jobId,
                                        UpdateOfferDecisionRequest request) {
        Offer offer = findOffer(userId, jobId);

        if (offer.getDecision() == request.decision()) {
            throw AppException.badRequest(
                    "Offer is already in decision: " + request.decision()
            );
        }

        offer.setDecision(request.decision());
        if (request.note() != null) offer.setNote(request.note());

        offer = offerRepository.save(offer);

        log.info("Offer decision updated: {} → {} for job: {}",
                offer.getDecision(), request.decision(), jobId);

        return toResponse(offer);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID jobId) {
        Offer offer = findOffer(userId, jobId);
        offer.softDelete();
        offerRepository.save(offer);
        log.info("Offer soft-deleted for job: {}", jobId);
    }

    private JobApplication findJob(UUID userId, UUID jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> AppException.notFound("Job application not found: " + jobId));
    }

    private Offer findOffer(UUID userId, UUID jobId) {
        return offerRepository.findByJobIdAndUserId(jobId, userId)
                .orElseThrow(() -> AppException.notFound("Offer not found for job: " + jobId));
    }

    private OfferResponse toResponse(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getJob().getId(),
                offer.getSalary(),
                offer.getCurrency(),
                offer.getBenefits(),
                offer.getStartDate(),
                offer.getExpiredAt(),
                offer.getDecision(),
                offer.getNote(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}