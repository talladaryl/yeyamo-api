package com.yeyamo_mobile.api.messaging_service.application;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yeyamo_mobile.api.messaging_service.application.MessagingDtos.ConversationView;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.OutingGroupLinkRepository;

/** Resolves the one durable outing-to-group link without exposing a group to
 * callers that are not active conversation members. */
@Service
public class OutingGroupAccessService {
    private static final Logger log = LoggerFactory.getLogger(OutingGroupAccessService.class);
    private final OutingGroupLinkRepository links;
    private final MessagingApplicationService conversations;

    public OutingGroupAccessService(OutingGroupLinkRepository links, MessagingApplicationService conversations) {
        this.links = links;
        this.conversations = conversations;
    }

    @Transactional(readOnly = true)
    public ConversationView resolveForMember(String actorId, UUID outingId) {
        var link = links.findById(outingId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Le groupe de cette sortie n'est pas encore disponible"));
        ConversationView group = conversations.get(actorId, link.getConversationId());
        log.info("event=OUTING_GROUP_RESOLVED outingId={} groupId={} actorId={}", outingId, group.id(), actorId);
        return group;
    }
}
