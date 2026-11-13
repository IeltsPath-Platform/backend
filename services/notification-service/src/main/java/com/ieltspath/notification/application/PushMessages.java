package com.ieltspath.notification.application;

import com.ieltspath.events.access.SubscriptionGrantedV1;
import com.ieltspath.events.assessment.GradingCompletedV1;
import com.ieltspath.events.assessment.GradingDueSoonV1;
import com.ieltspath.events.assessment.GradingOverdueV1;
import com.ieltspath.events.assessment.GradingRequestedV1;
import com.ieltspath.events.community.CommunityEventTypes;

import java.util.Map;

/**
 * Fixed Vietnamese text shown on the phone for each notification type. The text names no person and no content, so a
 * locked screen reveals nothing; the app opens the inbox item from the push data for details.
 */
final class PushMessages {
    record Text(String title, String body) { }

    private static final Text FALLBACK = new Text("Thông báo mới", "Bạn có một thông báo mới trên IELTSPath.");
    private static final Map<String, Text> BY_TYPE = Map.of(
            CommunityEventTypes.POST_COMMENTED, new Text("Bình luận mới", "Có người vừa bình luận bài viết của bạn."),
            CommunityEventTypes.COMMENT_REPLIED, new Text("Phản hồi mới", "Có người vừa trả lời bình luận của bạn."),
            CommunityEventTypes.COMMENT_MENTIONED, new Text("Bạn được nhắc đến", "Có người vừa nhắc đến bạn trong một bình luận."),
            CommunityEventTypes.POST_REACTED, new Text("Cảm xúc mới", "Có người vừa bày tỏ cảm xúc với bài viết của bạn."),
            CommunityEventTypes.USER_FOLLOWED, new Text("Người theo dõi mới", "Có người vừa theo dõi bạn."),
            GradingRequestedV1.EVENT_TYPE, new Text("Bài mới cần chấm", "Có một bài viết mới đang chờ bạn chấm."),
            GradingDueSoonV1.EVENT_TYPE, new Text("Sắp đến hạn chấm", "Một bài viết bạn nhận chấm sắp đến hạn."),
            GradingCompletedV1.EVENT_TYPE, new Text("Bài viết đã được chấm", "Giám khảo đã chấm xong bài viết của bạn."),
            GradingOverdueV1.EVENT_TYPE, new Text("Bài chấm bị trễ hạn", "Giám khảo chưa chấm bài viết của bạn đúng hạn."),
            SubscriptionGrantedV1.EVENT_TYPE, new Text("Đã kích hoạt Premium", "Gói Premium của bạn đã được kích hoạt."));

    private PushMessages() { }

    static Text forType(String eventType) { return BY_TYPE.getOrDefault(eventType, FALLBACK); }
}
