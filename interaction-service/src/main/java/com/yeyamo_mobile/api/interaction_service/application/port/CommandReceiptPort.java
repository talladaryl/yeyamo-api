package com.yeyamo_mobile.api.interaction_service.application.port;
import java.util.*;import com.yeyamo_mobile.api.interaction_service.domain.model.CommandReceipt;
public interface CommandReceiptPort{Optional<CommandReceipt> find(String key,String actor,String operation);CommandReceipt save(CommandReceipt receipt);}
