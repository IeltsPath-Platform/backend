package com.ieltspath.events.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpeakingGradingEventsJsonTest {
    private final ObjectMapper json=new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serializesRequestedItemsWithSnakeCaseContract() throws Exception {
        var event=new SpeakingGradingRequestedV1(id(1),SpeakingGradingRequestedV1.EVENT_TYPE,
                Instant.parse("2026-11-26T00:00:00Z"),SpeakingGradingRequestedV1.SOURCE,
                new SpeakingGradingRequestedV1.Data(id(2),id(3),List.of(
                        new SpeakingGradingRequestedV1.Item(id(4),2,3,"Describe a place","speaking/user/key.wav"))));
        byte[] bytes=json.writeValueAsBytes(event);
        assertEquals(json.readTree("""
                {"event_id":"30000000-0000-4000-8000-000000000001","event_type":"SpeakingGradingRequested.v1",
                 "occurred_at":"2026-11-26T00:00:00Z","source":"assessment-service","data":{
                  "attempt_id":"30000000-0000-4000-8000-000000000002","user_id":"30000000-0000-4000-8000-000000000003",
                  "items":[{"attempt_item_id":"30000000-0000-4000-8000-000000000004","part_no":2,"question_no":3,
                   "question":"Describe a place","object_key":"speaking/user/key.wav"}]}}
                """),json.readTree(bytes));
        assertEquals(event,json.readValue(bytes,SpeakingGradingRequestedV1.class));
    }

    @Test
    void serializesCompletedBandAndVietnameseFeedback() throws Exception {
        var event=new SpeakingGradingCompletedV1(id(5),SpeakingGradingCompletedV1.EVENT_TYPE,
                Instant.parse("2026-11-26T00:01:00Z"),SpeakingGradingCompletedV1.SOURCE,
                new SpeakingGradingCompletedV1.Data(id(2),id(3),"GRADED",new BigDecimal("6.5"),
                        List.of(new SpeakingGradingCompletedV1.Criterion("FC",new BigDecimal("7.0"),"Nói khá mạch lạc.")),
                        "Nhận xét tổng quan.","IELTS_SPEAKING",2,null));
        byte[] bytes=json.writeValueAsBytes(event);
        assertEquals(json.readTree("""
                {"event_id":"30000000-0000-4000-8000-000000000005","event_type":"SpeakingGradingCompleted.v1",
                 "occurred_at":"2026-11-26T00:01:00Z","source":"learning-service","data":{
                  "attempt_id":"30000000-0000-4000-8000-000000000002","user_id":"30000000-0000-4000-8000-000000000003",
                  "status":"GRADED","band":6.5,"criteria":[{"code":"FC","band":7.0,"comment":"Nói khá mạch lạc."}],
                  "summary":"Nhận xét tổng quan.","rubric_code":"IELTS_SPEAKING","rubric_version":2,"failure_code":null}}
                """),json.readTree(bytes));
        assertEquals(event,json.readValue(bytes,SpeakingGradingCompletedV1.class));
    }

    private static UUID id(int value){return UUID.fromString("30000000-0000-4000-8000-"+String.format("%012d",value));}
}
