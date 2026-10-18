package com.ieltspath.community.application.result;
import java.util.UUID;
import java.math.BigDecimal;
public record PublicProfile(UUID id,String displayName,String avatarUrl,String bio,BigDecimal targetBand,Integer streak) {
 public static PublicProfile anonymous(UUID id) { return new PublicProfile(id,"Học viên",null,null,null,null); }
}
