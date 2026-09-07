package com.yeyamo_mobile.api.media_service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.yeyamo_mobile.api.media_service.application.thumbnail.ImageThumbnailStrategy;

@SpringBootTest
@AutoConfigureMockMvc
class R2StorageIntegrationTest {
 private static final WireMockServer r2=new WireMockServer(wireMockConfig().dynamicPort());
 static {r2.start();}
 @Autowired private MockMvc mockMvc;
 @Autowired private ObjectMapper objectMapper;

 @BeforeAll static void start(){}
 @AfterAll static void stop(){if(r2!=null)r2.stop();}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("r2.endpoint-override",r2::baseUrl);}
 @BeforeEach void reset() throws Exception {
  r2.resetAll();
  r2.stubFor(put(urlPathMatching("/yeyamo-test/thumbnails/.*")).atPriority(1).willReturn(aResponse().withStatus(200).withHeader("ETag","\""+md5(thumbnail())+"\"")));
  r2.stubFor(put(urlPathMatching("/yeyamo-test/.*")).atPriority(2).willReturn(aResponse().withStatus(200).withHeader("ETag","\""+md5(png())+"\"")));
 }

 @Test void shouldUploadMediaToR2AndReturnId() throws Exception {
  String body=mockMvc.perform(upload("r2-upload-user"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty()).andReturn().getResponse().getContentAsString();
  UUID id=UUID.fromString(objectMapper.readTree(body).path("id").asText());
  r2.verify(putRequestedFor(urlPathMatching("/yeyamo-test/.*")));
  org.junit.jupiter.api.Assertions.assertNotNull(id);
 }

 @Test void shouldServeMediaContentFromR2() throws Exception {
  String body=mockMvc.perform(upload("r2-content-user")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  UUID id=UUID.fromString(objectMapper.readTree(body).path("id").asText());
  byte[] bytes=png();
  r2.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathMatching("/yeyamo-test/.*")).willReturn(aResponse().withStatus(200)
   .withHeader("Content-Type","image/png").withHeader("Content-Length",String.valueOf(bytes.length)).withBody(bytes)));
  mockMvc.perform(get("/api/v1/media/{id}/content",id))
   .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG)).andExpect(content().bytes(bytes));
 }

 @Test void shouldRejectUploadOnR2Failure() throws Exception {
  r2.resetAll();
  r2.stubFor(put(urlPathMatching("/yeyamo-test/.*")).willReturn(aResponse().withStatus(503).withBody("R2 unavailable")));
  mockMvc.perform(upload("r2-failure-user"))
   .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
 }

 private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder upload(String owner) throws Exception {
  return multipart("/api/v1/media").file(new MockMultipartFile("file","cover.png","image/png",png())).with(user(owner));
 }
 private static byte[] png() throws Exception {BufferedImage image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);ByteArrayOutputStream output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);return output.toByteArray();}
 private static byte[] thumbnail() throws Exception{return new ImageThumbnailStrategy(480,480).generate(png(),"image/png").bytes();}
 private static String md5(byte[] bytes) throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));}
}
