package com.group01.access.api.controller;

import com.group01.access.api.dto.response.KeyProductResponse;
import com.group01.access.api.dto.response.PlanResponse;
import com.group01.access.application.usecase.ListKeyProductsUseCase;
import com.group01.access.application.usecase.ListPlansUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class AccessCatalogController {

    private final ListPlansUseCase listPlansUseCase;
    private final ListKeyProductsUseCase listKeyProductsUseCase;

    @GetMapping("/plans")
    public List<PlanResponse> listPlans() {
        return listPlansUseCase.execute().stream()
                .map(PlanResponse::from)
                .toList();
    }

    @GetMapping("/key-products")
    public List<KeyProductResponse> listKeyProducts() {
        return listKeyProductsUseCase.execute().stream()
                .map(KeyProductResponse::from)
                .toList();
    }
}
