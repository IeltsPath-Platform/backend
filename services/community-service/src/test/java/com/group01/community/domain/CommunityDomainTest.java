package com.group01.community.domain;

import com.group01.community.domain.aggregate.*;
import com.group01.community.domain.exception.CommunityForbiddenException;
import com.group01.community.domain.vo.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CommunityDomainTest {
    @Test void postAuthorCanEditAndDelete(){UUID author=UUID.randomUUID();Post p=Post.create(author,PostCategory.GENERAL,"Title","Body");p.edit(author,PostCategory.QUESTION,null,"Updated");assertEquals("Updated",p.getBody());p.delete(author);assertEquals(ContentStatus.DELETED,p.getStatus());}
    @Test void nonAuthorCannotEditPost(){Post p=Post.create(UUID.randomUUID(),PostCategory.GENERAL,null,"Body");assertThrows(CommunityForbiddenException.class,()->p.edit(UUID.randomUUID(),PostCategory.GENERAL,null,"Other"));}
    @Test void moderationCannotRestoreDeletedPost(){UUID author=UUID.randomUUID();Post p=Post.create(author,PostCategory.GENERAL,null,"Body");p.delete(author);assertThrows(RuntimeException.class,()->p.moderate(ContentStatus.ACTIVE));}
    @Test void commentKeepsParentReference(){UUID parent=UUID.randomUUID();Comment c=Comment.create(UUID.randomUUID(),UUID.randomUUID(),parent,"Reply");assertEquals(parent,c.getParentCommentId());}
}
