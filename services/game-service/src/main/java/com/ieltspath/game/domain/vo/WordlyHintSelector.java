package com.ieltspath.game.domain.vo;
import java.util.List;
public final class WordlyHintSelector {
 private WordlyHintSelector(){}
 public static int firstUnknown(String answer,List<List<String>> history) {
  boolean[] known=new boolean[answer.length()];
  for(var row:history) {
   if(row.size()!=answer.length())throw new IllegalStateException("Invalid saved guess length");
   for(int i=0;i<row.size();i++)if("CORRECT".equals(row.get(i)))known[i]=true;
  }
  for(int i=0;i<known.length;i++)if(!known[i])return i;
  throw new IllegalArgumentException("All positions are already known");
 }
}
