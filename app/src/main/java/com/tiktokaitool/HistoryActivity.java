package com.tiktokaitool;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryActivity extends AppCompatActivity {
    private HistoryManager history;
    private TextView tvEmpty;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_history);
        history = new HistoryManager(this);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView rv = findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<HistoryManager.Item> list = history.getList();
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setAdapter(new Adapter(list));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_clear).setOnClickListener(v ->
            new AlertDialog.Builder(this).setTitle("Clear History")
                .setMessage("Delete all history?")
                .setPositiveButton("Delete",(d,w)->{ history.clear(); finish(); startActivity(getIntent()); })
                .setNegativeButton("Cancel",null).show());
    }

    class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        List<HistoryManager.Item> items;
        Adapter(List<HistoryManager.Item> l) { items=l; }
        @Override public VH onCreateViewHolder(ViewGroup p,int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_history,p,false));
        }
        @Override public void onBindViewHolder(VH h, int i) {
            HistoryManager.Item item = items.get(i);
            String txt = item.result.length()>100?item.result.substring(0,100)+"...":item.result;
            h.tvResult.setText(txt);
            h.tvTime.setText(new SimpleDateFormat("MMM dd, HH:mm",Locale.getDefault()).format(new Date(item.timestamp)));
            if (item.imagePath!=null&&!item.imagePath.isEmpty()) {
                Bitmap bmp = BitmapFactory.decodeFile(item.imagePath);
                h.ivThumb.setImageBitmap(bmp!=null?bmp:null);
            }
            h.itemView.setAlpha(0f);
            h.itemView.animate().alpha(1f).setDuration(300).setStartDelay(i*50L).start();
            h.itemView.setOnClickListener(v ->
                startActivity(new Intent(HistoryActivity.this,ResultActivity.class)
                    .putExtra("path",item.imagePath)
                    .putExtra("cached_result",item.result)));
        }
        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder {
            ImageView ivThumb; TextView tvResult, tvTime;
            VH(View v) { super(v); ivThumb=v.findViewById(R.id.iv_thumb); tvResult=v.findViewById(R.id.tv_result); tvTime=v.findViewById(R.id.tv_time); }
        }
    }
}
