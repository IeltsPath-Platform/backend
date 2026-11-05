package com.ieltspath.community.application.result;
import java.util.UUID;
import java.math.BigDecimal;
public record PublicProfile(UUID id,String displayName,String avatarUrl,String bio,BigDecimal targetBand,Integer streak) {
 public static PublicProfile anonymous(UUID id) { return new PublicProfile(id,"Học viên",null,null,null,null); }
 /** The same profile with the streak Learning reports; {@code null} hides it. */
 public PublicProfile withStreak(Integer streak) { return new PublicProfile(id,displayName,avatarUrl,bio,targetBand,streak); }
}
