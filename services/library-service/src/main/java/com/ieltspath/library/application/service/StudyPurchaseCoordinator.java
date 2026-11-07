package com.ieltspath.library.application.service;

import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.application.port.*;
import com.ieltspath.library.application.port.StudyGamificationRepository.Purchase;
import com.ieltspath.library.application.result.StudyGamificationResult.PurchaseResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import java.time.Clock;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StudyPurchaseCoordinator {
    private static final Logger log=LoggerFactory.getLogger(StudyPurchaseCoordinator.class);
    private final StudyGamificationRepository repository;
    private final StudyGamificationService gamification;
    private final StudyAccessClient access;
    private final StudyLock lock;
    private final Clock clock;
    private final TransactionTemplate transactions;
    public StudyPurchaseCoordinator(StudyGamificationRepository repository,StudyGamificationService gamification,StudyAccessClient access,StudyLock lock,Clock clock,PlatformTransactionManager manager) {
        this.repository=repository;this.gamification=gamification;this.access=access;this.lock=lock;this.clock=clock;this.transactions=new TransactionTemplate(manager);
    }
    public PurchaseResult purchase(UUID user,UUID request,String kind) {
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Study purchases must begin outside a transaction");
        if(!Set.of("HEART_REFILL","STREAK_FREEZE").contains(kind))throw new IllegalArgumentException("Unknown study purchase kind");
        Purchase intent=transactions.execute(status->{
            lock.lock(user);var existing=repository.purchase(request);
            if(existing.isPresent())return requireOwner(existing.get(),user,kind);
            int amount=gamification.checkPurchase(user,kind);repository.createPurchase(request,user,kind,amount,clock.instant());
            StudyMetrics.afterCommit("vocabulary_study_purchases","kind",kind,"status","PENDING");
            return requireOwner(repository.purchase(request).orElseThrow(),user,kind);
        });
        if(intent.status().equals("DONE"))return intent.result();
        log.info("Vocabulary study purchase requestId={} kind={} status=PENDING",request,kind);
        StudyAccessClient.PointsDebit debit;
        try {
            debit=access.debit(user,intent.amount(),kind,request,"vocab-study:"+request,kind.equals("HEART_REFILL")?"Hồi tim (từ vựng)":"Đóng băng streak (từ vựng)");
        } catch(StudyAccessClient.InsufficientPointsException e) {
            transactions.executeWithoutResult(status->{lock.lock(user);repository.deletePendingPurchase(request);});
            throw new StudyRequestException(HttpStatus.PAYMENT_REQUIRED,"INSUFFICIENT_POINTS","Bạn chưa đủ điểm để thực hiện giao dịch.");
        } catch(StudyAccessClient.PointsIdempotencyConflictException e) {
            throw StudyRequestException.conflict("IDEMPOTENCY_CONFLICT","requestId đã được dùng cho giao dịch khác.");
        } catch(StudyAccessClient.AccessUnavailableException e) {
            throw new StudyRequestException(HttpStatus.SERVICE_UNAVAILABLE,"ACCESS_UNAVAILABLE","Dịch vụ điểm đang bận. Vui lòng thử lại với cùng requestId.");
        }
        return transactions.execute(status->{
            lock.lock(user);var current=requireOwner(repository.purchase(request).orElseThrow(),user,kind);
            if(current.status().equals("DONE"))return current.result();
            var result=gamification.applyPurchase(user,kind,current.amount(),debit.balanceAfter());
            repository.finishPurchase(request,debit.ledgerId(),debit.balanceAfter(),result,clock.instant());
            StudyMetrics.afterCommit("vocabulary_study_purchases","kind",kind,"status","DONE");
            log.info("Vocabulary study purchase requestId={} kind={} status=DONE",request,kind);return result;
        });
    }
    private Purchase requireOwner(Purchase purchase,UUID user,String kind) {
        if(!purchase.userId().equals(user) || !purchase.kind().equals(kind))throw StudyRequestException.conflict("IDEMPOTENCY_CONFLICT","requestId đã được dùng cho giao dịch khác.");return purchase;
    }
}
