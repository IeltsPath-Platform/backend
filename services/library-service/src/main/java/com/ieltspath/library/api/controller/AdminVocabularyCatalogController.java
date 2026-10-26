package com.ieltspath.library.api.controller;

import com.ieltspath.library.application.usecase.VocabularyCatalogUseCase;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/admin/vocabulary")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CONTENT_AUTHOR','ADMIN')")
@Validated
public class AdminVocabularyCatalogController {
    private final VocabularyCatalogUseCase catalog;

    @PutMapping("/items/{id}") public Map<String,Object> updateItem(@PathVariable UUID id,@RequestBody @jakarta.validation.constraints.NotEmpty Map<String,Object> body) {
        Object rank=body.get("frequencyRank"); if(rank instanceof Number n && n.intValue()<0)throw new IllegalArgumentException("frequencyRank must be non-negative");
        Object awl=body.get("academicWordList"); if(awl!=null && !(awl instanceof Boolean))throw new IllegalArgumentException("academicWordList must be a boolean");
        return catalog.updateItem(id,body);
    }
    @PutMapping("/senses/{id}") public Map<String,Object> updateSense(@PathVariable UUID id,@RequestBody @jakarta.validation.constraints.NotEmpty Map<String,Object> body) {
        Object cefr=body.get("cefrLevel"); if(cefr!=null) { String normalized=cefr.toString().toUpperCase(); com.ieltspath.library.domain.vo.CefrLevel.valueOf(normalized); body.put("cefrLevel",normalized); }
        Object topic=body.get("topicId"); if(topic!=null) { if(topic.toString().isBlank())throw new IllegalArgumentException("topicId must be a UUID or null"); body.put("topicId",UUID.fromString(topic.toString())); }
        if(body.containsKey("register") && body.get("register")!=null && body.get("register").toString().length()>30)throw new IllegalArgumentException("register must be at most 30 characters");
        return catalog.updateSense(id,body);
    }
    @PutMapping("/senses/{id}/{kind}")
    public List<Map<String,Object>> replaceChildren(@PathVariable UUID id,@PathVariable String kind,@RequestBody @Size(max=30) List<Map<String,Object>> body) {
        if(!List.of("collocations","relations","common-mistakes","examples").contains(kind)) throw new IllegalArgumentException("unsupported child collection");
        if(body==null||body.size()>30)throw new IllegalArgumentException("at most 30 entries are allowed");
        body.forEach(row -> validateChild(kind,row));
        return catalog.replaceChildren(kind,id,body);
    }
    @GetMapping("/topics") public List<Map<String,Object>> topics() { return catalog.topicsAdmin(); }
    @PostMapping("/topics") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createTopic(@RequestBody @jakarta.validation.constraints.NotEmpty Map<String,Object> body) { validateTopic(body); return catalog.saveTopic(null,body); }
    @PutMapping("/topics/{id}") public Map<String,Object> updateTopic(@PathVariable UUID id,@RequestBody @jakarta.validation.constraints.NotEmpty Map<String,Object> body) { validateTopic(body); return catalog.saveTopic(id,body); }
    @DeleteMapping("/topics/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteTopic(@PathVariable UUID id) { catalog.deleteTopic(id); }
    @GetMapping("/sets") public List<Map<String,Object>> sets() { return catalog.setsAdmin(); }
    @PostMapping("/sets") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createSet(@RequestBody @jakarta.validation.constraints.NotEmpty Map<String,Object> body) { validateSet(body); return catalog.saveSet(null,body); }
    @PutMapping("/sets/{id}") public Map<String,Object> updateSet(@PathVariable UUID id,@RequestBody @jakarta.validation.constraints.NotEmpty Map<String,Object> body) { validateSet(body); return catalog.saveSet(id,body); }
    @DeleteMapping("/sets/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSet(@PathVariable UUID id) { catalog.deleteSet(id); }
    @PutMapping("/sets/{id}/items") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replaceSetItems(@PathVariable UUID id,@RequestBody @Size(max=500) List<UUID> senseIds) {
        if(senseIds==null||senseIds.size()>500)throw new IllegalArgumentException("at most 500 senses are allowed");
        if(senseIds.stream().distinct().count()!=senseIds.size())throw new IllegalArgumentException("senseIds must be unique");
        catalog.replaceSetItems(id,senseIds);
    }

    private void validateTopic(Map<String,Object> body) {
        required(body,"slug",100); required(body,"name",120); required(body,"nameVi",120);
        Object cover=body.get("coverKey"); if(cover!=null && cover.toString().startsWith("http"))throw new IllegalArgumentException("coverKey must be an object key");
    }
    private void validateSet(Map<String,Object> body) {
        required(body,"title",180); Object kind=body.get("kind"); if(kind==null || !List.of("BAND","TOPIC").contains(kind.toString().toUpperCase()))throw new IllegalArgumentException("kind must be BAND or TOPIC"); body.put("kind",kind.toString().toUpperCase());
        if("BAND".equalsIgnoreCase(kind.toString())) { Object cefr=body.get("cefrLevel"); if(cefr==null)throw new IllegalArgumentException("cefrLevel is required for BAND sets"); String normalized=cefr.toString().toUpperCase(); com.ieltspath.library.domain.vo.CefrLevel.valueOf(normalized); body.put("cefrLevel",normalized); if(body.get("topicId")!=null)throw new IllegalArgumentException("topicId is not allowed for BAND sets"); }
        else { Object topic=body.get("topicId"); if(topic==null)throw new IllegalArgumentException("topicId is required for TOPIC sets"); body.put("topicId",UUID.fromString(topic.toString())); if(body.get("cefrLevel")!=null)throw new IllegalArgumentException("cefrLevel is not allowed for TOPIC sets"); }
        Object cover=body.get("coverKey"); if(cover!=null && cover.toString().startsWith("http"))throw new IllegalArgumentException("coverKey must be an object key");
    }
    private void validateChild(String kind,Map<String,Object> row) {
        switch(kind) {
            case "collocations" -> required(row,"phrase",255);
            case "relations" -> { required(row,"relatedLemma",255); Object relation=row.get("kind"); if(relation==null||!List.of("SYNONYM","ANTONYM","RELATED").contains(relation.toString().toUpperCase()))throw new IllegalArgumentException("invalid relation kind"); row.put("kind",relation.toString().toUpperCase()); Object related=row.get("relatedItemId"); if(related!=null)row.put("relatedItemId",UUID.fromString(related.toString())); }
            case "common-mistakes" -> { required(row,"wrongUsage",10000); required(row,"correctUsage",10000); }
            case "examples" -> { required(row,"sentence",10000); required(row,"translationVi",10000); }
        }
    }
    private void required(Map<String,Object> body,String key,int max) { Object value=body.get(key); if(value==null||value.toString().isBlank()||value.toString().length()>max)throw new IllegalArgumentException(key+" is required and must not exceed "+max+" characters"); }
}
