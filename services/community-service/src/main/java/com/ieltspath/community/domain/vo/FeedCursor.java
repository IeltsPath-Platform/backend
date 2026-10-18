package com.ieltspath.community.domain.vo;
import com.ieltspath.community.domain.exception.CommunityException;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
public record FeedCursor(Instant createdAt, UUID id) {
 public static FeedCursor parse(String value) {
 if(value==null || value.isBlank())return null;
 try {
 if(value.length()>200)throw new IllegalArgumentException();
 var parts=new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8).split("\\|",-1);
 if(parts.length!=2)throw new IllegalArgumentException();
 return new FeedCursor(Instant.parse(parts[0]),UUID.fromString(parts[1]));
 }catch(RuntimeException e){throw new CommunityException("Invalid cursor");}
 }
 public String encode(){return Base64.getUrlEncoder().withoutPadding().encodeToString((createdAt+"|"+id).getBytes(StandardCharsets.UTF_8));}
}
