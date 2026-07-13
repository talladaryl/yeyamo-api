package com.yeyamo_mobile.api.media_service.application.thumbnail;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.*;import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
@Component
public class ImageThumbnailStrategy implements ThumbnailStrategy{
 private final int maxWidth,maxHeight;public ImageThumbnailStrategy(@Value("${media.thumbnail.max-width:480}")int w,@Value("${media.thumbnail.max-height:480}")int h){maxWidth=w;maxHeight=h;}
 public boolean supports(MediaType type){return type==MediaType.IMAGE;}
 public Thumbnail generate(byte[] original,String contentType){try{BufferedImage source=ImageIO.read(new ByteArrayInputStream(original));if(source==null)throw new IllegalArgumentException("Unsupported image encoding");
  double scale=Math.min(1d,Math.min((double)maxWidth/source.getWidth(),(double)maxHeight/source.getHeight()));int width=Math.max(1,(int)Math.round(source.getWidth()*scale));int height=Math.max(1,(int)Math.round(source.getHeight()*scale));
  BufferedImage target=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=target.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  g.setColor(Color.WHITE);g.fillRect(0,0,width,height);g.drawImage(source,0,0,width,height,null);g.dispose();ByteArrayOutputStream out=new ByteArrayOutputStream();
  if(!ImageIO.write(target,"jpeg",out))throw new IllegalStateException("JPEG writer unavailable");return new Thumbnail(out.toByteArray(),"image/jpeg",source.getWidth(),source.getHeight(),null);
 }catch(IOException e){throw new IllegalArgumentException("Cannot generate image thumbnail",e);}}
}
