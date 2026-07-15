package com.yeyamo_mobile.api.referral_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.referral_service.domain.InvitationStatus;import jakarta.persistence.*;
@Entity@Table(name="referral_invitations")public class ReferralInvitationEntity{
 @Id private UUID id;@ManyToOne(fetch=FetchType.EAGER,optional=false)@JoinColumn(name="code_id")private ReferralCodeEntity code;
 @Column(name="inviter_user_id",nullable=false,length=120)private String inviterUserId;@Column(name="email_hash",nullable=false,length=64)private String emailHash;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=30)private InvitationStatus status;@Column(name="invited_user_id",length=120)private String invitedUserId;
 @Column(name="created_at",nullable=false)private Instant createdAt;@Column(name="accepted_at")private Instant acceptedAt;@Version private long version;
 public static ReferralInvitationEntity create(ReferralCodeEntity code,String inviter,String emailHash){var e=new ReferralInvitationEntity();e.id=UUID.randomUUID();e.code=code;e.inviterUserId=inviter;e.emailHash=emailHash;e.status=InvitationStatus.PENDING;e.createdAt=Instant.now();return e;}
 public void accept(String user){status=InvitationStatus.ACCEPTED;invitedUserId=user;acceptedAt=Instant.now();}
 public UUID getId(){return id;}public ReferralCodeEntity getCode(){return code;}public String getInviterUserId(){return inviterUserId;}public String getEmailHash(){return emailHash;}public InvitationStatus getStatus(){return status;}public String getInvitedUserId(){return invitedUserId;}public Instant getCreatedAt(){return createdAt;}
}
