package com.yeyamo_mobile.api.catalog_service.application.port;
import java.util.*;
public interface PartnerArtworkAuthorizationPort { void requireCanManage(String userId, UUID partnerId); Set<UUID> manageablePartners(String userId); }
