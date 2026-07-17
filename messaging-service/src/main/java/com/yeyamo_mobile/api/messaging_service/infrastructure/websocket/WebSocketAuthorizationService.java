package com.yeyamo_mobile.api.messaging_service.infrastructure.websocket;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.messaging_service.domain.MemberStatus;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationMemberEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.ConversationMemberRepository;

/**
 * Service de vérification d'autorisation pour les souscriptions WebSocket.
 * 
 * Responsabilité: Vérifier qu'un utilisateur est membre actif d'une conversation
 * avant d'autoriser la souscription au canal correspondant.
 */
@Service
public class WebSocketAuthorizationService {
    
    private final ConversationMemberRepository memberRepository;
    
    // Pattern pour extraire conversationId des destinations privées
    // Ex: /user/queue/conversation.abc-123-def → conversationId = abc-123-def
    private static final Pattern CONVERSATION_PATTERN = 
        Pattern.compile("/user/queue/conversation\\.([a-f0-9\\-]+)");
    
    public WebSocketAuthorizationService(ConversationMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
    
    /**
     * Vérifie si un utilisateur est autorisé à souscrire à une destination.
     * 
     * @param userId ID de l'utilisateur (extrait du JWT)
     * @param destination Destination STOMP (ex: /user/queue/conversation.{id})
     * @return true si autorisé, false sinon
     */
    public boolean isAuthorizedToSubscribe(String userId, String destination) {
        // Si pas une destination conversation spécifique, autoriser
        // (ex: /user/queue/messaging pour notifications générales)
        if (destination == null || !destination.contains("conversation.")) {
            return true;
        }
        
        // Extraire conversationId
        UUID conversationId = extractConversationId(destination);
        if (conversationId == null) {
            // Destination mal formée → rejeter par sécurité
            return false;
        }
        
        // Vérifier appartenance
        return isMemberOfConversation(userId, conversationId);
    }
    
    /**
     * Extrait le conversationId d'une destination STOMP.
     * 
     * @param destination Destination STOMP
     * @return UUID de la conversation, ou null si non trouvé/invalide
     */
    private UUID extractConversationId(String destination) {
        Matcher matcher = CONVERSATION_PATTERN.matcher(destination);
        if (!matcher.find()) {
            return null;
        }
        
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            return null; // UUID invalide
        }
    }
    
    /**
     * Vérifie qu'un utilisateur est membre ACTIF d'une conversation.
     * 
     * @param userId ID utilisateur
     * @param conversationId ID conversation
     * @return true si membre actif, false sinon
     */
    private boolean isMemberOfConversation(String userId, UUID conversationId) {
        Optional<ConversationMemberEntity> member = 
            memberRepository.findByConversationIdAndUserId(conversationId, userId);
        
        // Membre non trouvé OU statut != ACTIVE → non autorisé
        return member.isPresent() 
            && member.get().getStatus() == MemberStatus.ACTIVE;
    }
}
