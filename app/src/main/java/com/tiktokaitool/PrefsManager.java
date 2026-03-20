package com.tiktokaitool;
import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private final SharedPreferences sp;
    public PrefsManager(Context ctx) { sp = ctx.getSharedPreferences("tikai", Context.MODE_PRIVATE); }
    public String getApiKey()  { return sp.getString("api_key",""); }
    public void setApiKey(String k) { sp.edit().putString("api_key",k).apply(); }
    public String getPrompt()  { return sp.getString("prompt", defaultPrompt()); }
    public void setPrompt(String p) { sp.edit().putString("prompt",p).apply(); }
    public String getModel()   { return sp.getString("model","claude-opus-4-5"); }
    public void setModel(String m)  { sp.edit().putString("model",m).apply(); }
    public boolean isAutoOpen(){ return sp.getBoolean("auto_open",true); }
    public void setAutoOpen(boolean v){ sp.edit().putBoolean("auto_open",v).apply(); }
    private String defaultPrompt() {
        return "Analyze this item from TikTok. Provide:\n1. Item name & brand\n2. Category\n3. Estimated price (USD)\n4. Where to buy\n5. Key features\n6. Similar alternatives\n7. Worth it? Yes/No + reason\n\nBe specific and helpful.";
    }
}
