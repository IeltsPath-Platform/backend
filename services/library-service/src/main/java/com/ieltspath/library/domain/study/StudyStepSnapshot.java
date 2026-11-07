package com.ieltspath.library.domain.study;
import java.util.*;

public record StudyStepSnapshot(UUID stepId,int order,String type,boolean graded,Map<String,Object> payload,Map<String,Object> answerKey) {}
