package com.ieltspath.library.domain.study;
public final class StudyXpRules {
    private StudyXpRules() {}
    public static int level(int totalXp,int levelXp){return totalXp/levelXp+1;}
    public static int nextLevelAt(int totalXp,int levelXp){return level(totalXp,levelXp)*levelXp;}
    public static int award(boolean first,boolean perfect,int completion,int bonus){return first?completion+(perfect?bonus:0):0;}
}
