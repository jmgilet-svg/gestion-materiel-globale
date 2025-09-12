package com.materiel.suite.client.net;

import java.util.*;

/**
 * Parser JSON minimaliste sans dépendance.
 * Conçu pour des payloads simples.
 */
public final class SimpleJson {
  private SimpleJson(){}

  public static Object parse(String json){
    return new P(json).parseAny();
  }
  @SuppressWarnings("unchecked")
  public static Map<String,Object> asObj(Object o){ return (Map<String,Object>) o; }
  @SuppressWarnings("unchecked")
  public static List<Object> asArr(Object o){ return (List<Object>) o; }
  public static String str(Object o){ return o==null? null : String.valueOf(o); }
  public static double num(Object o){ return o==null? 0 : (o instanceof Number n? n.doubleValue():Double.parseDouble(o.toString())); }
  public static boolean bool(Object o){ return o!=null && (o instanceof Boolean b? b:Boolean.parseBoolean(o.toString())); }

  private static final class P {
    private final String s; int i=0;
    P(String s){ this.s=s.trim(); }
    Object parseAny(){
      skipWs();
      if (i>=s.length()) return null;
      char c=s.charAt(i);
      if (c=='{') return parseObj();
      if (c=='[') return parseArr();
      if (c=='"') return parseStr();
      if (c=='t'||c=='f') return parseBool();
      if (c=='n'){ i+=4; return null; }
      return parseNum();
    }
    Map<String,Object> parseObj(){
      Map<String,Object> m=new LinkedHashMap<>();
      i++; skipWs();
      if (s.charAt(i)=='}'){ i++; return m; }
      while(true){
        String k=parseStr();
        expect(':');
        Object v=parseAny();
        m.put(k,v);
        skipWs();
        char c=s.charAt(i++);
        if (c=='}') break;
        if (c!=',') throw err("," );
      }
      return m;
    }
    List<Object> parseArr(){
      List<Object>a=new ArrayList<>();
      i++; skipWs();
      if (s.charAt(i)==']'){ i++; return a; }
      while(true){
        a.add(parseAny());
        skipWs();
        char c=s.charAt(i++);
        if (c==']') break;
        if (c!=',') throw err(",");
      }
      return a;
    }
    String parseStr(){
      expect('"');
      StringBuilder b=new StringBuilder();
      while(i<s.length()){
        char c=s.charAt(i++);
        if(c=='"') break;
        if(c=='\\'){
          char n=s.charAt(i++);
          if(n=='"'||n=='\\'||n=='/') b.append(n);
          else if(n=='b') b.append('\b');
          else if(n=='f') b.append('\f');
          else if(n=='n') b.append('\n');
          else if(n=='r') b.append('\r');
          else if(n=='t') b.append('\t');
          else if(n=='u'){
            String hex=s.substring(i,Math.min(i+4,s.length()));
            i+=4; b.append((char)Integer.parseInt(hex,16));
          } else b.append(n);
        } else b.append(c);
      }
      return b.toString();
    }
    Number parseNum(){
      int j=i;
      while(i<s.length() && "0123456789+-.eE".indexOf(s.charAt(i))>=0) i++;
      String sub=s.substring(j,i);
      if(sub.contains(".")||sub.contains("e")||sub.contains("E")) return Double.parseDouble(sub);
      try { return Integer.parseInt(sub);} catch(NumberFormatException e){ return Long.parseLong(sub); }
    }
    Boolean parseBool(){
      if(s.startsWith("true",i)){ i+=4; return Boolean.TRUE; }
      if(s.startsWith("false",i)){ i+=5; return Boolean.FALSE; }
      throw err("bool");
    }
    void expect(char c){
      skipWs();
      if(i>=s.length()||s.charAt(i)!=c) throw err("'"+c+"'");
      i++; skipWs();
    }
    void skipWs(){ while(i<s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    RuntimeException err(String m){ return new RuntimeException(m+" @"+i); }
  }
}
