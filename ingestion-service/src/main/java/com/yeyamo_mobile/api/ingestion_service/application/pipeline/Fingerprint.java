package com.yeyamo_mobile.api.ingestion_service.application.pipeline;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.text.Normalizer;
import java.util.Locale;
public final class Fingerprint {
    private Fingerprint(){}
    public static String of(String source,String external,String type,String name,Double lat,Double lng){
        String identity=external!=null&&!external.isBlank()?source+"|"+external:
                normalize(type)+"|"+normalize(name)+"|"+round(lat)+"|"+round(lng);
        try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static String normalize(String v){return v==null?"":Normalizer.normalize(v,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();}
    private static String round(Double v){return v==null?"":String.format(Locale.ROOT,"%.5f",v);}
}
