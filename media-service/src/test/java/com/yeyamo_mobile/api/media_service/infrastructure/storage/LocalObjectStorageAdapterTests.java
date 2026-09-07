package com.yeyamo_mobile.api.media_service.infrastructure.storage;
import static org.junit.jupiter.api.Assertions.*;import java.io.*;import java.nio.file.Path;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
class LocalObjectStorageAdapterTests{
 @TempDir Path root;
 @Test void storesReadsAndDeletes()throws Exception{var storage=new LocalObjectStorageAdapter(root.toString());byte[] bytes="hello".getBytes();
  storage.store("originals/a.txt",new ByteArrayInputStream(bytes),bytes.length,"text/plain");var object=storage.open("originals/a.txt");assertArrayEquals(bytes,object.content().readAllBytes());
  storage.delete("originals/a.txt");assertThrows(com.yeyamo_mobile.api.media_service.application.MediaException.class,()->storage.open("originals/a.txt"));}
 @Test void blocksTraversal(){var storage=new LocalObjectStorageAdapter(root.toString());assertThrows(com.yeyamo_mobile.api.media_service.application.MediaException.class,()->storage.open("../secret"));}
}
