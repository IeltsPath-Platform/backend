package com.group01.assessment.application.result;
import java.util.List; import java.util.UUID;
public record AttemptStructureResult(List<Section> sections){ public record Section(UUID id,UUID contentSectionId,int sortOrder,String snapshot,List<Item> items){} public record Item(UUID id,UUID questionVersionId,int sortOrder,String questionSnapshot,String answerSnapshot,String knowledgeSnapshot){} }
