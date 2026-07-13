package com.yeyamo_mobile.api.media_service.application;
import static org.junit.jupiter.api.Assertions.*;import java.awt.image.BufferedImage;import java.io.*;import java.util.*;import javax.imageio.ImageIO;import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.media_service.application.port.*;import com.yeyamo_mobile.api.media_service.application.thumbnail.*;import com.yeyamo_mobile.api.media_service.domain.model.*;import com.yeyamo_mobile.api.media_service.domain.port.MediaRepository;
class MediaApplicationServiceTests{
 @Test void uploadStoresMetadataThumbnailAndEvents()throws Exception{MemoryRepository repo=new MemoryRepository();MemoryStorage storage=new MemoryStorage();List<String> events=new ArrayList<>();
  byte[] png=image();MediaApplicationService service=new MediaApplicationService(repo,storage,(e,m,c,a)->events.add(e),new MediaContentPolicy(100000,100000),List.of(new ImageThumbnailStrategy(100,100)));
  MediaAsset media=service.upload("user-1","photo.png","image/png",png,"Vue","post","p1","corr");
  assertEquals(MediaStatus.READY,media.getStatus());assertEquals(ThumbnailStatus.READY,media.getThumbnailStatus());assertNotNull(media.getThumbnailKey());
  assertEquals(List.of("media.uploaded","media.ready"),events);assertEquals(2,storage.data.size());}
 @Test void duplicateIsRejected()throws Exception{MemoryRepository repo=new MemoryRepository();MemoryStorage storage=new MemoryStorage();MediaApplicationService service=new MediaApplicationService(repo,storage,(a,b,c,d)->{},new MediaContentPolicy(100000,100000),List.of(new ImageThumbnailStrategy(100,100)));
  byte[] png=image();service.upload("u","a.png","image/png",png,null,null,null,null);assertThrows(MediaException.class,()->service.upload("u","b.png","image/png",png,null,null,null,null));}
 private static byte[] image()throws Exception{BufferedImage i=new BufferedImage(20,10,BufferedImage.TYPE_INT_RGB);ByteArrayOutputStream o=new ByteArrayOutputStream();ImageIO.write(i,"png",o);return o.toByteArray();}
 static class MemoryRepository implements MediaRepository{Map<UUID,MediaAsset> data=new HashMap<>();public MediaAsset save(MediaAsset m){data.put(m.getId(),m);return m;}public Optional<MediaAsset> findById(UUID id){return Optional.ofNullable(data.get(id));}
  public boolean existsByChecksumAndOwnerId(String c,String o){return data.values().stream().anyMatch(m->m.getChecksum().equals(c)&&m.getOwnerId().equals(o)&&m.getStatus()!=MediaStatus.DELETED);}}
 static class MemoryStorage implements ObjectStoragePort{Map<String,byte[]>data=new HashMap<>();public String store(String k,InputStream i,long l,String c){try{data.put(k,i.readAllBytes());return k;}catch(Exception e){throw new RuntimeException(e);}}
  public StoredObject open(String k){return new StoredObject(new ByteArrayInputStream(data.get(k)),data.get(k).length,"application/octet-stream");}public void delete(String k){data.remove(k);}}
}
