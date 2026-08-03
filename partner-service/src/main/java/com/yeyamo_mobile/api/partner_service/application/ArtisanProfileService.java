package com.yeyamo_mobile.api.partner_service.application;

import static com.yeyamo_mobile.api.partner_service.interfaces.rest.ArtisanDtos.*;
import com.yeyamo.foundation.domain.*;
import com.yeyamo_mobile.api.partner_service.application.port.PartnerOutboxPort;
import com.yeyamo_mobile.api.partner_service.domain.model.*;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.*;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.ArtisanRepositories.*;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class ArtisanProfileService {
    private static final Set<BusinessType> TYPES = Set.of(BusinessType.ARTISAN, BusinessType.ARTIST, BusinessType.CULTURAL_ASSOCIATION, BusinessType.MUSEUM, BusinessType.CULTURAL_EXPERT);
    private final Profiles profiles; private final Specialties specialties; private final SpringPartnerRepository partners;
    private final SpringPartnerDocumentRepository documents; private final KycRequirements requirements; private final PartnerOutboxPort outbox;
    public ArtisanProfileService(Profiles profiles, Specialties specialties, SpringPartnerRepository partners, SpringPartnerDocumentRepository documents, KycRequirements requirements, PartnerOutboxPort outbox) { this.profiles=profiles;this.specialties=specialties;this.partners=partners;this.documents=documents;this.requirements=requirements;this.outbox=outbox; }

    public Response create(String actor, Request request, String correlation) {
        PartnerEntity partner=owned(actor); if(profiles.existsById(partner.getId())) throw new PartnerException("ARTISAN_PROFILE_EXISTS","Le profil artisan existe déjà",HttpStatus.CONFLICT); validate(request); partner.setBusinessType(request.artisanType());
        ArtisanProfileEntity profile=ArtisanProfileEntity.create(partner.getId(),request.displayName(),request.story(),request.craftDescription(),request.yearsOfExperience(),new CountryReference(request.countryCode()).countryCode(),request.adminLevel1Id(),request.cityId(),request.localityId(),languages(request.languages()),specialties(request.specialtyIds()),request.acceptsCustomOrders(),request.internationalShipping());
        profiles.save(profile); event("ArtisanProfileCreated",partner.getId(),actor,correlation,profile); return Response.from(profile);
    }
    @Transactional(readOnly=true) public Response me(String actor){return Response.from(profiles.findById(owned(actor).getId()).orElseThrow(this::missing));}
    public Response update(String actor,Request request,String correlation){PartnerEntity partner=owned(actor);ArtisanProfileEntity profile=profiles.findById(partner.getId()).orElseThrow(this::missing);validate(request);partner.setBusinessType(request.artisanType());profile.update(request.displayName(),request.story(),request.craftDescription(),request.yearsOfExperience(),new CountryReference(request.countryCode()).countryCode(),request.adminLevel1Id(),request.cityId(),request.localityId(),languages(request.languages()),specialties(request.specialtyIds()),request.acceptsCustomOrders(),request.internationalShipping());event("ArtisanProfileUpdated",partner.getId(),actor,correlation,profile);return Response.from(profile);}
    @Transactional(readOnly=true) public Page<Response> search(String country,String area,String city,UUID specialty,Boolean verified,Boolean custom,Boolean shipping,String search,Pageable pageable){Specification<ArtisanProfileEntity>spec=(root,q,cb)->{q.distinct(true);List<Predicate>filters=new ArrayList<>();filters.add(cb.notEqual(root.get("verificationStatus"),ArtisanProfileEntity.VerificationStatus.SUSPENDED));if(country!=null&&!country.isBlank())filters.add(cb.equal(root.get("countryCode"),new CountryReference(country).countryCode()));if(area!=null&&!area.isBlank())filters.add(cb.equal(root.get("adminLevel1Id"),area));if(city!=null&&!city.isBlank())filters.add(cb.equal(root.get("cityId"),city));if(specialty!=null)filters.add(cb.equal(root.join("specialties").get("id"),specialty));if(Boolean.TRUE.equals(verified))filters.add(cb.equal(root.get("verificationStatus"),ArtisanProfileEntity.VerificationStatus.VERIFIED));if(custom!=null)filters.add(cb.equal(root.get("acceptsCustomOrders"),custom));if(shipping!=null)filters.add(cb.equal(root.get("internationalShipping"),shipping));if(search!=null&&!search.isBlank()){String like="%"+search.toLowerCase().trim()+"%";filters.add(cb.or(cb.like(cb.lower(root.get("displayName")),like),cb.like(cb.lower(root.get("craftDescription")),like)));}return cb.and(filters.toArray(Predicate[]::new));};return profiles.findAll(spec,pageable).map(Response::from);}
    @Transactional(readOnly=true) public Response publicProfile(UUID id){return Response.from(profiles.findById(id).filter(value->value.getVerificationStatus()==ArtisanProfileEntity.VerificationStatus.VERIFIED).orElseThrow(this::missing));}
    @Transactional(readOnly=true) public List<Specialty> specialties(){return specialties.findByActiveTrueOrderByName().stream().map(Specialty::from).toList();}
    public Response verify(UUID id,VerificationRequest request,String actor,String correlation){ArtisanProfileEntity profile=profiles.findById(id).orElseThrow(this::missing);if(!Set.of(ArtisanProfileEntity.VerificationStatus.VERIFIED,ArtisanProfileEntity.VerificationStatus.REJECTED,ArtisanProfileEntity.VerificationStatus.SUSPENDED,ArtisanProfileEntity.VerificationStatus.PENDING).contains(request.status()))throw new PartnerException("INVALID_ARTISAN_STATUS","Statut invalide",HttpStatus.BAD_REQUEST);if(request.status()==ArtisanProfileEntity.VerificationStatus.VERIFIED)validateKyc(profile);profile.verification(request.status());String type=switch(request.status()){case VERIFIED->"ArtisanVerified";case SUSPENDED->"ArtisanSuspended";default->"ArtisanProfileUpdated";};event(type,id,actor,correlation,profile);return Response.from(profile);}
    private void validateKyc(ArtisanProfileEntity profile){PartnerEntity partner=partners.findById(profile.getPartnerId()).orElseThrow(()->new PartnerException("PARTNER_NOT_FOUND","Partenaire introuvable",HttpStatus.NOT_FOUND));Set<DocumentType>provided=documents.findByPartnerIdOrderByUploadedAtDesc(profile.getPartnerId()).stream().map(PartnerDocumentEntity::getDocumentType).collect(Collectors.toSet());Set<DocumentType>required=requirements.findByCountryCodeAndPartnerTypeAndRequiredTrue(profile.getCountryCode(),partner.getBusinessType()).stream().map(ArtisanKycRequirementEntity::getDocumentType).collect(Collectors.toSet());if(!provided.containsAll(required)){Set<DocumentType>missing=new HashSet<>(required);missing.removeAll(provided);throw new PartnerException("ARTISAN_KYC_INCOMPLETE","Documents artisanaux manquants: "+missing,HttpStatus.CONFLICT);}}
    private PartnerEntity owned(String actor){return partners.findByOwnerUserId(actor).orElseThrow(()->new PartnerException("PARTNER_NOT_FOUND","Créez d'abord un partenaire",HttpStatus.NOT_FOUND));}
    private void validate(Request request){if(!TYPES.contains(request.artisanType()))throw new PartnerException("ARTISAN_TYPE_INVALID","Type de partenaire artisan invalide",HttpStatus.BAD_REQUEST);new CountryReference(request.countryCode());request.languages().forEach(LanguageReference::new);}
    private Set<String> languages(Set<String>values){return values.stream().map(value->new LanguageReference(value).languageCode()).collect(Collectors.toSet());}
    private Set<ArtisanSpecialtyEntity> specialties(Set<UUID>ids){List<ArtisanSpecialtyEntity>values=specialties.findByIdInAndActiveTrue(ids);if(values.size()!=ids.size())throw new PartnerException("ARTISAN_SPECIALTY_INVALID","Spécialité inconnue ou désactivée",HttpStatus.BAD_REQUEST);return new HashSet<>(values);}
    private PartnerException missing(){return new PartnerException("ARTISAN_PROFILE_NOT_FOUND","Profil artisan introuvable",HttpStatus.NOT_FOUND);}
    private void event(String type,UUID id,String actor,String correlation,ArtisanProfileEntity profile){outbox.append(type,id,actor,correlation,Map.of("partnerId",id,"countryCode",profile.getCountryCode(),"verificationStatus",profile.getVerificationStatus().name()));}
}
