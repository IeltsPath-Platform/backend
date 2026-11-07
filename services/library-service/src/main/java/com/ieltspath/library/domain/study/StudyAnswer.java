package com.ieltspath.library.domain.study;
import java.util.*;

public record StudyAnswer(String type,Boolean acknowledged,UUID optionId,List<Pair> pairs,String text) {
    public record Pair(UUID leftOptionId,UUID rightOptionId) {}
}
