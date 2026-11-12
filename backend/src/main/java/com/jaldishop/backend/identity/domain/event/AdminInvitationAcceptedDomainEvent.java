package com.jaldishop.backend.identity.domain.event;

import java.util.UUID;

public record AdminInvitationAcceptedDomainEvent(
        UUID userId,
        String email,
        UUID invitedBy
) {}
