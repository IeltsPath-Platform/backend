package com.ieltspath.notification.infrastructure.push;

import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.notification.application.NotificationEventTypes;

public record PushCopy(String title, String body) {
    private static final PushCopy DEFAULT = new PushCopy("IELTSPath", "Bạn có thông báo mới trên IELTSPath.");

    public static PushCopy forEventType(String eventType) {
        if (eventType == null) return DEFAULT;
        return switch (eventType) {
            case CommunityEventTypes.POST_REACTED -> new PushCopy("IELTSPath", "Có người vừa bày tỏ cảm xúc về bài viết của bạn.");
            case CommunityEventTypes.POST_COMMENTED -> new PushCopy("IELTSPath", "Có người vừa bình luận về bài viết của bạn.");
            case CommunityEventTypes.COMMENT_REPLIED -> new PushCopy("IELTSPath", "Có người vừa trả lời bình luận của bạn.");
            case CommunityEventTypes.COMMENT_MENTIONED -> new PushCopy("IELTSPath", "Có người vừa nhắc đến bạn trong một bình luận.");
            case CommunityEventTypes.USER_FOLLOWED -> new PushCopy("IELTSPath", "Bạn có một người theo dõi mới.");
            case NotificationEventTypes.GRADING_COMPLETED -> new PushCopy("Bài của bạn đã được chấm", "Giám khảo đã chấm xong, mở IELTSPath để xem điểm và nhận xét.");
            case NotificationEventTypes.RESULT_READY -> new PushCopy("Đã có kết quả bài thi", "Kết quả bài thi của bạn đã sẵn sàng.");
            case NotificationEventTypes.GRADING_OVERDUE -> new PushCopy("Bài chấm đang chậm", "Bài làm đang được chấm chậm hơn dự kiến.");
            case NotificationEventTypes.GRADING_REQUESTED -> new PushCopy("Có bài mới cần chấm", "Một bài làm vừa được giao cho bạn chấm.");
            case NotificationEventTypes.GRADING_DUE_SOON -> new PushCopy("Bài chấm sắp đến hạn", "Một bài bạn đang chấm sắp hết hạn.");
            case NotificationEventTypes.SUBSCRIPTION_GRANTED -> new PushCopy("Gói học đã được kích hoạt", "Bạn đã có quyền truy cập nội dung của gói vừa mua.");
            case NotificationEventTypes.SUBSCRIPTION_EXPIRY_NOTICE -> new PushCopy("Gói học sắp hết hạn", "Gia hạn để không bị gián đoạn việc học.");
            case NotificationEventTypes.VOCABULARY_REVIEW_DUE -> new PushCopy("Đến giờ ôn từ vựng", "Bạn có từ vựng cần ôn hôm nay. Ôn ngay để nhớ lâu hơn.");
            default -> DEFAULT;
        };
    }
}
