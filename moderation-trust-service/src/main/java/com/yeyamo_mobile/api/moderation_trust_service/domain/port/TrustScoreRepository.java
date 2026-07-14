package com.yeyamo_mobile.api.moderation_trust_service.domain.port;import java.util.Optional;import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TrustScore;
public interface TrustScoreRepository{TrustScore save(TrustScore s);Optional<TrustScore>findById(String id);}
