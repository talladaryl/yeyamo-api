package com.yeyamo_mobile.api.interaction_service.domain.port;
import java.util.*;import com.yeyamo_mobile.api.interaction_service.domain.model.CheckIn;
public interface CheckInRepository{CheckIn save(CheckIn checkIn);Optional<CheckIn> findById(UUID id);List<CheckIn> findByUser(String userId,int limit);}
