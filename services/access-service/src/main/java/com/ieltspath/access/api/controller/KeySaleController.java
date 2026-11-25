package com.ieltspath.access.api.controller;

import com.ieltspath.access.api.dto.request.KeyDistributionRequests;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.CodeResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.CountsResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.IssuedKeyResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.KeyResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.PageResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.StockResponse;
import com.ieltspath.access.application.port.KeyDistributionStore.State;
import com.ieltspath.access.application.usecase.SaleKeyUseCases;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The sales staff's side: only the keys held for the caller, handed out one at a time. */
@RestController
@RequestMapping("/api/access/sale")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALES_STAFF')")
public class KeySaleController {
    private final CurrentUserProvider currentUser;
    private final SaleKeyUseCases keys;

    @GetMapping("/stock")
    public StockResponse stock() {
        SaleKeyUseCases.Stock stock = keys.stock(currentUser.requireUserId());
        return new StockResponse(stock.byProduct().stream().map(CountsResponse::from).toList(),
                stock.byBatch().stream().map(CountsResponse::from).toList());
    }

    /** Takes the next key of the product. Asking again with the same Idempotency-Key returns the same key. */
    @PostMapping("/issue")
    public ResponseEntity<IssuedKeyResponse> issue(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                   @Valid @RequestBody KeyDistributionRequests.Issue request) {
        SaleKeyUseCases.IssuedCode issued = keys.issue(currentUser.requireUserId(), request.productId(), idempotencyKey);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new IssuedKeyResponse(issued.keyId(), issued.code(), issued.batchCode(), KeyResponse.from(issued.key())));
    }

    @GetMapping("/issued")
    public PageResponse<KeyResponse> issued(@RequestParam(value = "productId", required = false) UUID productId,
                                            @RequestParam(value = "state", required = false) State state,
                                            @RequestParam(value = "q", required = false) String q,
                                            @RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "size", defaultValue = "20") int size) {
        return PageResponse.from(keys.issued(currentUser.requireUserId(), productId, state, q, page, size), KeyResponse::from);
    }

    @GetMapping("/keys/{id}")
    public KeyResponse key(@PathVariable("id") UUID id) {
        return KeyResponse.from(keys.key(currentUser.requireUserId(), id));
    }

    @PostMapping("/keys/{id}/reveal")
    public ResponseEntity<CodeResponse> reveal(@PathVariable("id") UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CodeResponse(keys.reveal(currentUser.requireUserId(), id)));
    }
}
