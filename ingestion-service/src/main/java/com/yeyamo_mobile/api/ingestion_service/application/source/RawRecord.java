package com.yeyamo_mobile.api.ingestion_service.application.source;
import java.util.Map;
public record RawRecord(int rowNumber,Map<String,String> values){}
