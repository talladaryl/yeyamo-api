package com.yeyamo_mobile.api.media_service.application.thumbnail;
import static org.junit.jupiter.api.Assertions.*;import java.awt.image.BufferedImage;import java.io.*;import javax.imageio.ImageIO;import org.junit.jupiter.api.Test;
class ImageThumbnailStrategyTests{
 @Test void generatesBoundedJpeg()throws Exception{BufferedImage image=new BufferedImage(1000,500,BufferedImage.TYPE_INT_RGB);ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
  var thumb=new ImageThumbnailStrategy(200,200).generate(out.toByteArray(),"image/png");assertEquals("image/jpeg",thumb.contentType());assertEquals(1000,thumb.width());assertEquals(500,thumb.height());
  BufferedImage generated=ImageIO.read(new ByteArrayInputStream(thumb.bytes()));assertEquals(200,generated.getWidth());assertEquals(100,generated.getHeight());}
}
