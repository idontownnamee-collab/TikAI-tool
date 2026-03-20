package com.tiktokaitool;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private final SharedPreferences sp;
    public HistoryManager(Context ctx) { sp = ctx.getSharedPreferences("tikai_history", Context.MODE_PRIVATE); }

    public void save(String img, String result) {
        try {
            JSONArray arr = getAll();
            JSONObject o = new JSONObject().put("image",img).put("result",result).put("time",System.currentTimeMillis());
            JSONArray n = new JSONArray();
            n.put(o);
            for (int i=0;i<Math.min(arr.length(),99);i++) n.put(arr.get(i));
            sp.edit().putString("history",n.toString()).apply();
        } catch(Exception e){ e.printStackTrace(); }
    }

    public JSONArray getAll() {
        try { return new JSONArray(sp.getString("history","[]")); }
        catch(Exception e){ return new JSONArray(); }
    }

    public void clear() { sp.edit().remove("history").apply(); }

    public List<Item> getList() {
        List<Item> list = new ArrayList<>();
        try {
            JSONArray arr = getAll();
            for(int i=0;i<arr.length();i++){
                JSONObject o = arr.getJSONObject(i);
                list.add(new Item(o.optString("image"),o.optString("result"),o.optLong("time")));
            }
        } catch(Exception e){}
        return list;
    }

    public static class Item {
        public String imagePath, result; public long timestamp;
        public Item(String i, String r, long t){ imagePath=i; result=r; timestamp=t; }
    }
}
