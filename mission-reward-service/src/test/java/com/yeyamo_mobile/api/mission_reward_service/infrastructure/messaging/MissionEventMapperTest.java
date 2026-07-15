package com.yeyamo_mobile.api.mission_reward_service.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;import com.fasterxml.jackson.databind.ObjectMapper;import org.junit.jupiter.api.*;
class MissionEventMapperTest{
 private final ObjectMapper mapper=new ObjectMapper();private final MissionEventMapper subject=new MissionEventMapper(mapper);
 @Test void mapsGamificationContract()throws Exception{var e=subject.map(mapper.readTree("{\"eventId\":\"11111111-1111-1111-1111-111111111111\",\"eventType\":\"gamification.xp.awarded\",\"eventVersion\":1,\"occurredAt\":\"2026-07-14T10:00:00Z\",\"correlationId\":\"c1\",\"payload\":{\"userId\":\"u1\",\"points\":30}}"));assertEquals("u1",e.userId());assertEquals(30,e.payload().get("points"));}
 @Test void mapsInteractionAuthor()throws Exception{var e=subject.map(mapper.readTree("{\"eventId\":\"11111111-1111-1111-1111-111111111111\",\"eventType\":\"interaction.comment.created\",\"occurredAt\":\"2026-07-14T10:00:00Z\",\"payload\":{\"authorId\":\"u2\"}}"));assertEquals("u2",e.userId());}
 @Test void rejectsEventWithoutUser()throws Exception{var node=mapper.readTree("{\"eventId\":\"11111111-1111-1111-1111-111111111111\",\"eventType\":\"x\",\"occurredAt\":\"2026-07-14T10:00:00Z\",\"payload\":{}}");assertThrows(IllegalArgumentException.class,()->subject.map(node));}
}
