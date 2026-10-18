package com.ieltspath.game.api.controller;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.application.usecase.UseWordlyHintUseCase;
import com.ieltspath.game.api.dto.response.WordlyHintResponse;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
@RestController
@RequestMapping("/api/games/matches")
public class WordlyHintController {
 private final CurrentUserProvider user;private final UseWordlyHintUseCase hint;
 public WordlyHintController(CurrentUserProvider user,UseWordlyHintUseCase hint){this.user=user;this.hint=hint;}
 @PostMapping("/{matchId}/words/{playerWordId}/hint")
 public WordlyHintResponse use(@PathVariable("matchId")UUID matchId,@PathVariable("playerWordId")UUID wordId,@Valid @RequestBody Request request){return WordlyHintResponse.from(hint.execute(matchId,wordId,user.requireUserId(),request.requestId()));}
 public record Request(@NotNull UUID requestId){}
}
