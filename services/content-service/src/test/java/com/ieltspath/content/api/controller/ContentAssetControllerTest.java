package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.response.ContentAssetResponse;
import com.ieltspath.content.application.result.ContentAssetResult;
import com.ieltspath.content.application.usecase.CreateContentAssetUseCase;
import com.ieltspath.content.application.usecase.GetContentAssetUseCase;
import com.ieltspath.content.application.usecase.LinkAssetUseCase;
import com.ieltspath.content.domain.vo.AccessTier;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.AssetValidationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentAssetControllerTest {
    private final CreateContentAssetUseCase createAsset = mock(CreateContentAssetUseCase.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ContentAssetController(
            mock(GetContentAssetUseCase.class), createAsset, mock(LinkAssetUseCase.class))).build();

    @Test
    void createsPremiumLessonAudioWithTheSelectedTier() throws Exception {
        UUID id = UUID.randomUUID();
        when(createAsset.execute(any())).thenReturn(new ContentAssetResult(id, AssetType.AUDIO, "Transcript",
                "listening/demo/ls1.mp3", 45, null, AssetValidationStatus.VALID, Instant.now(), AccessTier.PREMIUM));

        mockMvc.perform(post("/api/content/assets").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetType":"AUDIO","textContent":"Transcript",
                                 "mediaReference":"listening/demo/ls1.mp3","durationSeconds":45,
                                 "accessTier":"PREMIUM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessTier").value("PREMIUM"));

        var command = org.mockito.ArgumentCaptor.forClass(
                com.ieltspath.content.application.command.CreateContentAssetCommand.class);
        verify(createAsset).execute(command.capture());
        org.assertj.core.api.Assertions.assertThat(command.getValue().accessTier()).isEqualTo(AccessTier.PREMIUM);
    }

    @Test
    void newAssetsDefaultToPublicAccess() throws Exception {
        UUID id = UUID.randomUUID();
        when(createAsset.execute(any())).thenReturn(new ContentAssetResult(id, AssetType.AUDIO, null,
                "listening/demo/ls1.mp3", 45, null, AssetValidationStatus.VALID, Instant.now(), AccessTier.PUBLIC));

        mockMvc.perform(post("/api/content/assets").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetType":"AUDIO","mediaReference":"listening/demo/ls1.mp3",
                                 "durationSeconds":45}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessTier").value("PUBLIC"));

        var command = org.mockito.ArgumentCaptor.forClass(
                com.ieltspath.content.application.command.CreateContentAssetCommand.class);
        verify(createAsset).execute(command.capture());
        org.assertj.core.api.Assertions.assertThat(command.getValue().accessTier()).isEqualTo(AccessTier.PUBLIC);
    }
}
