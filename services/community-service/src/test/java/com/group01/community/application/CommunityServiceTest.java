package com.group01.community.application;

import com.group01.commonsecurity.currentuser.*;
import com.group01.commonsecurity.role.CanonicalRoles;
import com.group01.community.domain.aggregate.Post;
import com.group01.community.domain.exception.CommunityForbiddenException;
import com.group01.community.domain.repository.*;
import com.group01.community.domain.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityServiceTest {
    private final PostRepository posts=mock(PostRepository.class);
    private final CommentRepository comments=mock(CommentRepository.class);
    private final ReactionRepository reactions=mock(ReactionRepository.class);
    private final CurrentUserProvider users=mock(CurrentUserProvider.class);
    private final CommunityService service=new CommunityService(posts,comments,reactions,users);

    @Test void adminCanHidePost(){UUID admin=UUID.randomUUID();Post post=Post.create(UUID.randomUUID(),PostCategory.GENERAL,null,"body");when(users.requireCurrentUser()).thenReturn(new CurrentUser(admin,Set.of(CanonicalRoles.ADMIN)));when(posts.findById(post.getId())).thenReturn(Optional.of(post));when(posts.save(post)).thenReturn(post);assertEquals(ContentStatus.HIDDEN,service.moderatePost(post.getId(),ContentStatus.HIDDEN).getStatus());}
    @Test void nonAdminCannotModerate(){when(users.requireCurrentUser()).thenReturn(new CurrentUser(UUID.randomUUID(),Set.of(CanonicalRoles.CUSTOMER)));assertThrows(CommunityForbiddenException.class,()->service.moderatePost(UUID.randomUUID(),ContentStatus.HIDDEN));verifyNoInteractions(posts);}
    @Test void paginationLimitIsEnforced(){assertThrows(RuntimeException.class,()->service.listPosts(0,101));verifyNoInteractions(posts);}
}
