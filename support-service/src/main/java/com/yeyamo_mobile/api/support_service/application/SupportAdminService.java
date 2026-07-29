package com.yeyamo_mobile.api.support_service.application;

import static com.yeyamo_mobile.api.support_service.application.SupportDtos.*;
import java.time.*;import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.support_service.domain.*;
import com.yeyamo_mobile.api.support_service.infrastructure.persistence.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;import org.springframework.data.jpa.domain.Specification;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class SupportAdminService {
 private final SupportConversationRepository conversations; private final SupportMessageRepository messages; private final SupportInternalNoteRepository notes; private final SupportAuditRepository audits; private final ObjectMapper mapper;
 public SupportAdminService(SupportConversationRepository c,SupportMessageRepository m,SupportInternalNoteRepository n,SupportAuditRepository a,ObjectMapper mapper){conversations=c;messages=m;notes=n;audits=a;this.mapper=mapper;}
 @Transactional(readOnly=true) public Page<ConversationSummary> list(SupportStatus status,SupportPriority priority,String assignee,String user,String search,Instant from,Instant to,Pageable pageable){
  Specification<SupportConversationEntity> spec=(root,q,cb)->{List<Predicate> p=new ArrayList<>();if(status!=null)p.add(cb.equal(root.get("status"),status));if(priority!=null)p.add(cb.equal(root.get("priority"),priority));if(text(assignee)!=null)p.add(cb.equal(root.get("assigneeAdminId"),assignee));if(text(user)!=null)p.add(cb.equal(root.get("userId"),user));if(text(search)!=null){String like="%"+search.trim().toLowerCase(Locale.ROOT)+"%";p.add(cb.or(cb.like(cb.lower(root.get("subject")),like),cb.like(cb.lower(root.get("userId")),like)));}if(from!=null)p.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from));if(to!=null)p.add(cb.lessThanOrEqualTo(root.get("createdAt"),to));return cb.and(p.toArray(Predicate[]::new));};
  return conversations.findAll(spec,pageable).map(this::summary);
 }
 @Transactional(readOnly=true) public ConversationDetail detail(UUID id){var c=find(id);return new ConversationDetail(summary(c),messages.findForConversation(id).stream().map(this::message).toList(),notes.findForConversation(id).stream().map(n->new NoteResponse(n.id,n.authorAdminId,n.content,n.createdAt)).toList());}
 public MessageResponse message(UUID id,String actor,MessageRequest request,String correlation){var c=find(id);Instant now=Instant.now();var m=new SupportMessageEntity();m.id=UUID.randomUUID();m.conversationId=id;m.senderType="ADMIN";m.senderId=actor;m.content=request.content().trim();m.attachmentMediaIds=write(request.attachmentMediaIds()==null?List.of():request.attachmentMediaIds());m.createdAt=now;messages.save(m);if(c.firstResponseAt==null)c.firstResponseAt=now;c.updatedAt=now;conversations.save(c);audit(id,actor,"SUPPORT_MESSAGE_SENT",correlation,"{}");return message(m);}
 public NoteResponse note(UUID id,String actor,NoteRequest request,String correlation){find(id);var n=new SupportInternalNoteEntity();n.id=UUID.randomUUID();n.conversationId=id;n.authorAdminId=actor;n.content=request.content().trim();n.createdAt=Instant.now();notes.save(n);audit(id,actor,"SUPPORT_INTERNAL_NOTE_ADDED",correlation,"{}");return new NoteResponse(n.id,n.authorAdminId,n.content,n.createdAt);}
 public ConversationSummary assign(UUID id,String actor,AssignRequest request,String correlation){var c=find(id);c.assigneeAdminId=request.assigneeId();c.updatedAt=Instant.now();audit(id,actor,"SUPPORT_ASSIGNED",correlation,write(Map.of("assigneeId",request.assigneeId())));return summary(conversations.save(c));}
 public ConversationSummary status(UUID id,String actor,StatusRequest request,String correlation){var c=find(id);c.status=request.status();c.updatedAt=Instant.now();c.resolvedAt=request.status()==SupportStatus.RESOLVED?c.updatedAt:null;audit(id,actor,"SUPPORT_STATUS_CHANGED",correlation,write(Map.of("status",request.status(),"reason",Objects.toString(request.reason(),""))));return summary(conversations.save(c));}
 public ConversationSummary priority(UUID id,String actor,PriorityRequest request,String correlation){var c=find(id);c.priority=request.priority();c.updatedAt=Instant.now();audit(id,actor,"SUPPORT_PRIORITY_CHANGED",correlation,write(Map.of("priority",request.priority(),"reason",Objects.toString(request.reason(),""))));return summary(conversations.save(c));}
 private SupportConversationEntity find(UUID id){return conversations.findById(id).orElseThrow(()->new NoSuchElementException("Support conversation not found"));}
 private ConversationSummary summary(SupportConversationEntity c){return new ConversationSummary(c.id,c.userId,c.subject,c.status,c.priority,c.assigneeAdminId,c.createdAt,c.updatedAt,c.firstResponseAt,c.resolvedAt,seconds(c.createdAt,c.firstResponseAt),seconds(c.createdAt,c.resolvedAt));}
 private MessageResponse message(SupportMessageEntity m){return new MessageResponse(m.id,m.senderType,m.senderId,m.content,read(m.attachmentMediaIds),m.createdAt);}
 private void audit(UUID id,String actor,String action,String correlation,String metadata){var a=new SupportAuditEntity();a.id=UUID.randomUUID();a.conversationId=id;a.actorId=actor;a.action=action;a.metadata=metadata;a.correlationId=correlation;a.createdAt=Instant.now();audits.save(a);}
 private String write(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("Invalid metadata",e);}}
 private List<String> read(String value){try{return mapper.readValue(value,new TypeReference<>(){});}catch(Exception e){return List.of();}}
 private Long seconds(Instant a,Instant b){return b==null?null:Duration.between(a,b).getSeconds();} private String text(String s){return s==null||s.isBlank()?null:s.trim();}
}
