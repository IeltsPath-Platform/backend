package com.ieltspath.library.application.service;

import com.fasterxml.jackson.databind.*;
import com.ieltspath.library.domain.study.StudyAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Component @RequiredArgsConstructor
public class StudyRequestHash {
    private final ObjectMapper mapper;
    public String start(UUID unit,String mode){return hash(Map.of("unitId",unit,"mode",mode));}
    public String answer(UUID attempt,UUID step,StudyAnswer answer) {
        if(answer!=null && answer.pairs()!=null && answer.pairs().stream().allMatch(Objects::nonNull))
            answer=new StudyAnswer(answer.type(),answer.acknowledged(),answer.optionId(),answer.pairs().stream().sorted(Comparator.comparing(p->Objects.toString(p.leftOptionId(),""))).toList(),answer.text());
        var value=new TreeMap<String,Object>();value.put("attemptId",attempt);value.put("stepId",step);value.put("answer",answer);return hash(value);
    }
    private String hash(Object value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsString(canonical(mapper.valueToTree(value))).getBytes(StandardCharsets.UTF_8)));}
        catch(java.io.IOException|NoSuchAlgorithmException e){throw new IllegalStateException("Cannot hash study request",e);}
    }
    private Object canonical(JsonNode node) {
        if(node.isObject()){var map=new TreeMap<String,Object>();node.fields().forEachRemaining(e->map.put(e.getKey(),canonical(e.getValue())));return map;}
        if(node.isArray()){var list=new ArrayList<Object>();node.forEach(v->list.add(canonical(v)));return list;}
        return node;
    }
}
