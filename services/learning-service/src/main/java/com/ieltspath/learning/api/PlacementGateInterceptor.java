package com.ieltspath.learning.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.RequirePlacementUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Keeps a learner out of every learning endpoint until a placement exists. Other roles pass through so their own
 * authorization still answers.
 */
public class PlacementGateInterceptor implements HandlerInterceptor {
    private static final String LEARNER_ROLE = "CUSTOMER";

    private final CurrentUserProvider currentUser;
    private final RequirePlacementUseCase requirePlacement;

    public PlacementGateInterceptor(CurrentUserProvider currentUser, RequirePlacementUseCase requirePlacement) {
        this.currentUser = currentUser;
        this.requirePlacement = requirePlacement;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        currentUser.currentUser()
                .filter(user -> user.hasRole(LEARNER_ROLE))
                .ifPresent(user -> requirePlacement.execute(user.id()));
        return true;
    }
}
