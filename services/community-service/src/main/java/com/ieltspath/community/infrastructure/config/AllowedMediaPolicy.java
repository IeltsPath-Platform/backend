package com.ieltspath.community.infrastructure.config;
import com.ieltspath.community.application.port.MediaPolicy;
import com.ieltspath.community.domain.exception.CommunityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;
@Component
public class AllowedMediaPolicy implements MediaPolicy {
 private final Set<String> imageHosts;
 private final Set<String> videoHosts;
 public AllowedMediaPolicy(@Value("${community.media.image-hosts:images.unsplash.com,i.imgur.com}") String images,
 @Value("${community.media.video-hosts:youtube.com,www.youtube.com,youtu.be,loom.com,www.loom.com,drive.google.com}") String videos) {
 imageHosts=hosts(images); videoHosts=hosts(videos);
 }
 private static Set<String> hosts(String text) { return new HashSet<>(Arrays.stream(text.split(",")).map(String::trim).map(s->s.toLowerCase(Locale.ROOT)).filter(s->!s.isEmpty()).toList()); }
 public void validate(List<String> images,String video) {
 if(images!=null) images.forEach(url->validateUrl(url,imageHosts));
 if(video!=null) validateUrl(video,videoHosts);
 }
 private static void validateUrl(String value,Set<String> hosts) {
 try {
 var u=URI.create(value);
 if(value.length()>500 || !"https".equalsIgnoreCase(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null
 || (u.getPort()!=-1 && u.getPort()!=443) || !hosts.contains(u.getHost().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException();
 } catch(RuntimeException e) { throw new CommunityException("Media URL must use HTTPS and an allowed host"); }
 }
}
