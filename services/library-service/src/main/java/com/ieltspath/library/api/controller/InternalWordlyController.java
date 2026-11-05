package com.ieltspath.library.api.controller;
import com.ieltspath.library.application.usecase.GetRandomWordlyWordsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/internal/game-content/wordly")
public class InternalWordlyController {
    private final GetRandomWordlyWordsUseCase words;
    public InternalWordlyController(GetRandomWordlyWordsUseCase words){this.words=words;}
    @PostMapping("/random")
    public Response random(@Valid @RequestBody Request request) {
        return new Response(words.execute(request.count()).stream().map(w->new Item(w.sourceWordId(),w.answer(),w.meaning())).toList());
    }
    @PostMapping("/catalog")
    public Response catalog(@Valid @RequestBody PageRequest request) {
        return new Response(words.page(request.after()==null?"":request.after(),request.limit(),request.cefrLevel(),request.topicId()).stream().map(w->new Item(w.sourceWordId(),w.answer(),w.meaning())).toList());
    }
    @GetMapping("/filters")
    public FiltersResponse filters() {
        return new FiltersResponse(words.topics().stream().map(t->new TopicItem(t.id(),t.slug(),t.name(),t.nameVi())).toList(),
            words.availability().stream().map(a->new AvailabilityItem(a.cefrLevel(),a.topicId(),a.words())).toList());
    }
    public record PageRequest(String after,@NotNull @Min(1) @jakarta.validation.constraints.Max(500) Integer limit,
        @jakarta.validation.constraints.Pattern(regexp="A1|A2|B1|B2|C1|C2") String cefrLevel,UUID topicId){}
    public record TopicItem(UUID id,String slug,String name,String nameVi){}
    public record AvailabilityItem(String cefrLevel,UUID topicId,int words){}
    public record FiltersResponse(List<TopicItem> topics,List<AvailabilityItem> availability){}
    public record Request(@NotNull @Min(1) @jakarta.validation.constraints.Max(100) Integer count){}
    public record Item(UUID sourceWordId,String answer,String meaning){}
    public record Response(List<Item> words){}
}
