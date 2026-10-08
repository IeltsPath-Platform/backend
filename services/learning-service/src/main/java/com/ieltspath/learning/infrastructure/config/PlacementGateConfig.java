package com.ieltspath.learning.infrastructure.config;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.PlacementGateInterceptor;
import com.ieltspath.learning.api.controller.PlacementController;
import com.ieltspath.learning.application.usecase.RequirePlacementUseCase;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Applies the placement gate to every learning endpoint except the placement test itself. */
@Configuration
public class PlacementGateConfig implements WebMvcConfigurer {
    private final CurrentUserProvider currentUser;
    private final RequirePlacementUseCase requirePlacement;

    public PlacementGateConfig(CurrentUserProvider currentUser, RequirePlacementUseCase requirePlacement) {
        this.currentUser = currentUser;
        this.requirePlacement = requirePlacement;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PlacementGateInterceptor(currentUser, requirePlacement))
                .addPathPatterns("/api/learning/**")
                .excludePathPatterns(PlacementController.PATH);
    }
}
