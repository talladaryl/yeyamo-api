package com.yeyamo_mobile.api.culture_service.application;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public final class YouTubeUrlValidator {
    private static final Set<String> HOSTS=Set.of("youtube.com","www.youtube.com","youtu.be","www.youtube-nocookie.com");
    private static final Pattern ID=Pattern.compile("[A-Za-z0-9_-]{11}");
    private YouTubeUrlValidator(){}
    public static String normalize(String value){
        if(value==null||value.isBlank())return null;
        try{
            URI uri=URI.create(value.trim());String host=uri.getHost()==null?null:uri.getHost().toLowerCase();
            if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||!HOSTS.contains(host)||uri.getUserInfo()!=null)throw invalid();
            String videoId=null;
            if(host.equals("youtu.be")){String path=uri.getPath();if(path!=null&&path.length()>1&&!path.substring(1).contains("/"))videoId=path.substring(1);}
            else if(uri.getPath().equals("/watch")){for(String part:java.util.Optional.ofNullable(uri.getRawQuery()).orElse("").split("&")){String[] pair=part.split("=",2);if(pair.length==2&&pair[0].equals("v"))videoId=pair[1];}}
            else if(host.equals("www.youtube-nocookie.com")&&uri.getPath().startsWith("/embed/"))videoId=uri.getPath().substring(7);
            if(videoId==null||!ID.matcher(videoId).matches())throw invalid();
            return "https://www.youtube-nocookie.com/embed/"+videoId;
        }catch(CultureException e){throw e;}catch(RuntimeException e){throw invalid();}
    }
    private static CultureException invalid(){return new CultureException("INVALID_YOUTUBE_URL","URL YouTube invalide",HttpStatus.BAD_REQUEST);}
}
