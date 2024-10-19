package com.hanz.youmetalk;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class IconTextAdapter extends ArrayAdapter<String> {

    private Context context;
    private String[] items;
    private Drawable[] icons;

    public IconTextAdapter(@NonNull Context context, String[] items, int[] iconResIds) {
        super(context, R.layout.dialog_item_layout, items);
        this.context = context;
        this.items = items;

        // Convert int[] to Drawable[]
        icons = new Drawable[iconResIds.length];
        for (int i = 0; i < iconResIds.length; i++) {
            icons[i] = ContextCompat.getDrawable(context, iconResIds[i]);
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.dialog_item_layout, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.dialog_text);
        ImageView imageView = convertView.findViewById(R.id.dialog_icon);

        textView.setText(items[position]);
        imageView.setImageDrawable(icons[position]);

        return convertView;
    }
}