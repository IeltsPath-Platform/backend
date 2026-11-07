package com.ieltspath.library.application.port;
import java.time.Instant;
import java.util.UUID;
public interface StudyStreakActivity {void recordActivity(UUID userId,Instant at);}
