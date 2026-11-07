package com.ieltspath.library.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.ZoneId;

@Component
@ConfigurationProperties("library.vocabulary-study")
public class StudyGamificationProperties {
    private final Xp xp=new Xp();
    private final Hearts hearts=new Hearts();
    private final Streak streak=new Streak();
    private String defaultTimezone="Asia/Ho_Chi_Minh";
    public Xp getXp(){return xp;}
    public Hearts getHearts(){return hearts;}
    public Streak getStreak(){return streak;}
    public String getDefaultTimezone(){return defaultTimezone;}
    public void setDefaultTimezone(String value){if(value==null || !ZoneId.getAvailableZoneIds().contains(value))throw new IllegalArgumentException("Study timezone must be an IANA timezone");defaultTimezone=value;}
    public static class Xp {
        private int firstCompletion=20;
        public int getFirstCompletion(){return firstCompletion;}
        public void setFirstCompletion(int value){if(value<1)throw new IllegalArgumentException("firstCompletion must be at least 1");firstCompletion=value;}
        private int perfectBonus=10;
        public int getPerfectBonus(){return perfectBonus;}
        public void setPerfectBonus(int value){if(value<0)throw new IllegalArgumentException("perfectBonus must be at least 0");perfectBonus=value;}
        private int levelXp=500;
        public int getLevelXp(){return levelXp;}
        public void setLevelXp(int value){if(value<1)throw new IllegalArgumentException("levelXp must be at least 1");levelXp=value;}
    }
    public static class Hearts {
        private int max=5;
        public int getMax(){return max;}
        public void setMax(int value){if(value<1)throw new IllegalArgumentException("max must be at least 1");max=value;}
        private int premiumMax=7;
        public int getPremiumMax(){return premiumMax;}
        public void setPremiumMax(int value){if(value<1)throw new IllegalArgumentException("premiumMax must be at least 1");premiumMax=value;}
        private int regenHours=5;
        public int getRegenHours(){return regenHours;}
        public void setRegenHours(int value){if(value<1)throw new IllegalArgumentException("regenHours must be at least 1");regenHours=value;}
        private int refillCost=5;
        public int getRefillCost(){return refillCost;}
        public void setRefillCost(int value){if(value<1)throw new IllegalArgumentException("refillCost must be at least 1");refillCost=value;}
        private int planRefreshMinutes=10;
        public int getPlanRefreshMinutes(){return planRefreshMinutes;}
        public void setPlanRefreshMinutes(int value){if(value<1)throw new IllegalArgumentException("planRefreshMinutes must be at least 1");planRefreshMinutes=value;}
    }
    public static class Streak {
        private int freezePrice=10;
        public int getFreezePrice(){return freezePrice;}
        public void setFreezePrice(int value){if(value<1)throw new IllegalArgumentException("freezePrice must be at least 1");freezePrice=value;}
        private int maxConsecutiveFreezes=5;
        public int getMaxConsecutiveFreezes(){return maxConsecutiveFreezes;}
        public void setMaxConsecutiveFreezes(int value){if(value<1)throw new IllegalArgumentException("maxConsecutiveFreezes must be at least 1");maxConsecutiveFreezes=value;}
    }
}
