package com.ieltspath.learning.infrastructure.cache;

import com.ieltspath.learning.application.port.LearningContentClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TopicCacheShapeTest {
    private static final Map<Class<?>, List<String>> CACHE_SHAPE = Map.of(
            LearningContentClient.Topic.class, List.of(
                    "topicId:UUID", "code:String", "name:String", "sortOrder:int", "requiredFeatureKey:String",
                    "knowledgePoints:List<KnowledgePoint>", "skill:LearningSkill", "hasTopicTest:boolean", "course:Course",
                    "skills:Set<LearningSkill>", "active:Boolean", "lessonCount:int", "description:String"),
            LearningContentClient.Course.class, List.of(
                    "courseId:UUID", "code:String", "name:String", "bandLevel:BigDecimal", "hasCourseTest:boolean",
                    "active:Boolean", "coverImageUrl:String", "description:String", "skillUnlockPoints:Map<LearningSkill,Integer>",
                    "comboUnlockPoints:Integer", "availableSkills:List<LearningSkill>", "bannerImageUrl:String"),
            LearningContentClient.KnowledgePoint.class, List.of(
                    "id:UUID", "code:String", "name:String", "learningType:String", "skill:String",
                    "description:String", "hasPracticeSet:boolean", "cefrLevel:String", "canDo:String",
                    "parentKnowledgePointId:UUID", "errorCode:String", "earliestLessonPosition:Integer"));

    @Test
    void changingSerializedFieldsRequiresANewCacheKeyVersion() {
        CACHE_SHAPE.forEach((record, expectedShape) -> {
            List<String> actual = java.util.Arrays.stream(record.getRecordComponents())
                    .map(TopicCacheShapeTest::shape).toList();
            assertThat(actual).as("If %s changes, bump topic-sequence cache key v1 to v2", record.getSimpleName())
                    .containsExactlyElementsOf(expectedShape);
        });
    }

    private static String shape(RecordComponent component) {
        String type = component.getType().getSimpleName();
        if (component.getGenericType() instanceof ParameterizedType parameterized) {
            String arguments = java.util.Arrays.stream(parameterized.getActualTypeArguments())
                    .map(argument -> argument instanceof Class<?> argumentClass
                            ? argumentClass.getSimpleName() : argument.getTypeName())
                    .collect(java.util.stream.Collectors.joining(","));
            type += "<" + arguments + ">";
        }
        return component.getName() + ":" + type;
    }
}
