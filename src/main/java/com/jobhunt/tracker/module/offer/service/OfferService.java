package com.jobhunt.tracker.module.offer.service;

import com.jobhunt.tracker.module.offer.dto.*;

import java.util.UUID;

public interface OfferService {

    OfferResponse create(UUID userId, UUID jobId, CreateOfferRequest request);

    OfferResponse get(UUID userId, UUID jobId);

    OfferResponse update(UUID userId, UUID jobId, UpdateOfferRequest request);

    OfferResponse updateDecision(UUID userId, UUID jobId, UpdateOfferDecisionRequest request);

    void delete(UUID userId, UUID jobId);
}