package com.ieltspath.community.application.result;
import java.util.List;
public record CursorResult<T>(List<T> content,String nextCursor,boolean hasNext,int size) {
 public CursorResult { content=List.copyOf(content); }
}
