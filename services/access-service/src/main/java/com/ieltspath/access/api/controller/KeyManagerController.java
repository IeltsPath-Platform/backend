package com.ieltspath.access.api.controller;

import com.ieltspath.access.api.dto.request.KeyDistributionRequests;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.ActionResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.BatchResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.CodeResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.CountsResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.IssuedKeyResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.KeyResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.OverviewResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.PageResponse;
import com.ieltspath.access.api.dto.response.KeyDistributionResponses.SaleResponse;
import com.ieltspath.access.application.port.KeyDistributionStore.Allocation;
import com.ieltspath.access.application.port.KeyDistributionStore.State;
import com.ieltspath.access.application.usecase.KeyBatchUseCases;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** The sales manager's side: batches, giving keys to sales, taking them back, revoking, handing out and exporting. */
@RestController
@RequestMapping("/api/access/manager")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF_MANAGER')")
public class KeyManagerController {
    /** Lets Excel open the file as UTF-8. */
    private static final String BYTE_ORDER_MARK = "﻿";

    private final CurrentUserProvider currentUser;
    private final KeyBatchUseCases keys;

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse createBatch(@Valid @RequestBody KeyDistributionRequests.CreateBatch request) {
        return BatchResponse.from(keys.createBatch(currentUser.requireUserId(), request.productId(), request.quantity(),
                request.expiresOn(), request.label()));
    }

    @GetMapping("/batches")
    public PageResponse<BatchResponse> batches(@RequestParam(value = "productId", required = false) UUID productId,
                                               @RequestParam(value = "month", required = false) String month,
                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                               @RequestParam(value = "size", defaultValue = "20") int size) {
        return PageResponse.from(keys.listBatches(productId, month, page, size), BatchResponse::from);
    }

    @GetMapping("/batches/{code}")
    public BatchResponse batch(@PathVariable("code") String code) {
        return BatchResponse.from(keys.batch(code));
    }

    @GetMapping("/batches/{code}/keys")
    public PageResponse<KeyResponse> batchKeys(@PathVariable("code") String code,
                                               @RequestParam(value = "state", required = false) State state,
                                               @RequestParam(value = "saleId", required = false) UUID saleId,
                                               @RequestParam(value = "pool", defaultValue = "false") boolean pool,
                                               @RequestParam(value = "held", defaultValue = "false") boolean held,
                                               @RequestParam(value = "q", required = false) String q,
                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                               @RequestParam(value = "size", defaultValue = "20") int size) {
        return PageResponse.from(keys.batchKeys(code, state, saleId, pool, held, q, page, size), KeyResponse::from);
    }

    @PostMapping("/batches/{code}/allocations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void allocate(@PathVariable("code") String code, @Valid @RequestBody KeyDistributionRequests.Allocate request) {
        keys.allocate(currentUser.requireUserId(), code, request.allocations().stream()
                .map(line -> new Allocation(line.saleId(), line.quantity())).toList());
    }

    @PostMapping("/batches/{code}/revoke-unused")
    public ActionResponse revokeUnused(@PathVariable("code") String code,
                                       @Valid @RequestBody(required = false) KeyDistributionRequests.RevokeUnused request) {
        return ActionResponse.from(keys.revokeUnusedInBatch(currentUser.requireUserId(), code, request == null ? null : request.note()));
    }

    /** The batch as a CSV with full codes. Every download is written to the audit log. */
    @GetMapping("/batches/{code}/export")
    public ResponseEntity<byte[]> export(@PathVariable("code") String code) {
        String csv = BYTE_ORDER_MARK + keys.exportCsv(currentUser.requireUserId(), code);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + code + ".csv\"")
                .cacheControl(CacheControl.noStore())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/keys/reassign")
    public ActionResponse reassign(@Valid @RequestBody KeyDistributionRequests.Reassign request) {
        return ActionResponse.from(keys.reassign(currentUser.requireUserId(), request.keyIds(), request.toSaleId()));
    }

    @PostMapping("/keys/withdraw")
    public ActionResponse withdraw(@Valid @RequestBody KeyDistributionRequests.Keys request) {
        return ActionResponse.from(keys.withdraw(currentUser.requireUserId(), request.keyIds()));
    }

    @PostMapping("/keys/revoke")
    public ActionResponse revoke(@Valid @RequestBody KeyDistributionRequests.Revoke request) {
        return ActionResponse.from(keys.revoke(currentUser.requireUserId(), request.keyIds(), request.note()));
    }

    /** POST because looking at a code leaves an audit row; the answer must never be cached. */
    @PostMapping("/keys/{id}/reveal")
    public ResponseEntity<CodeResponse> reveal(@PathVariable("id") UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CodeResponse(keys.reveal(currentUser.requireUserId(), id)));
    }

    @PostMapping("/issue")
    public ResponseEntity<IssuedKeyResponse> issue(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                   @Valid @RequestBody KeyDistributionRequests.Issue request) {
        KeyBatchUseCases.IssuedCode issued = keys.issueDirect(currentUser.requireUserId(), request.productId(), idempotencyKey);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new IssuedKeyResponse(issued.keyId(), issued.code(), issued.batchCode(), KeyResponse.from(issued.key())));
    }

    @GetMapping("/overview")
    public OverviewResponse overview(@RequestParam(value = "month", required = false) String month) {
        KeyBatchUseCases.Overview overview = keys.overview(month);
        return new OverviewResponse(overview.byBatch().stream().map(CountsResponse::from).toList(),
                overview.byProduct().stream().map(CountsResponse::from).toList());
    }

    @GetMapping("/sales")
    public List<SaleResponse> sales() {
        return keys.sales().stream().map(SaleResponse::from).toList();
    }
}
