package com.ieltspath.library.domain.study;
import java.util.*;

public final class UnitUnlockRules {
    private UnitUnlockRules() {}
    public static List<String> statuses(List<UUID> units,Set<UUID> completed,Set<UUID> active) {
        var result=new ArrayList<String>();
        for(int i=0;i<units.size();i++) {
            UUID id=units.get(i);result.add(completed.contains(id)?"COMPLETED":active.contains(id)?"IN_PROGRESS":i==0 || completed.contains(units.get(i-1))?"AVAILABLE":"LOCKED");
        }
        return result;
    }
}
