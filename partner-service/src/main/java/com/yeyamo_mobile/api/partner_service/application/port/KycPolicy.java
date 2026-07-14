package com.yeyamo_mobile.api.partner_service.application.port;
import java.util.List;import com.yeyamo_mobile.api.partner_service.domain.model.*;
public interface KycPolicy{void validate(Partner partner,List<PartnerDocument>documents);}
