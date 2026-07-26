package com.yeyamo_mobile.api.partner_service.infrastructure.staff;
import org.springframework.data.jpa.repository.*;import java.util.*;
interface PartnerRoleRepository extends JpaRepository<PartnerRoleEntity,UUID>{List<PartnerRoleEntity>findByPartnerId(UUID p);}
interface PartnerMembershipRepository extends JpaRepository<PartnerMembershipEntity,UUID>{Optional<PartnerMembershipEntity>findByPartnerIdAndUserId(UUID p,String u);List<PartnerMembershipEntity>findByPartnerId(UUID p);}
interface PartnerInvitationRepository extends JpaRepository<PartnerInvitationEntity,UUID>{Optional<PartnerInvitationEntity>findByTokenHash(String h);}
