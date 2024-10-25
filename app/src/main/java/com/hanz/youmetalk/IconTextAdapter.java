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
/**
 * IconTextAdapter is a custom ArrayAdapter designed to display a list of text items alongside icons
 * within a dialog or list view. Each item displays an icon next to the corresponding text, enhancing
 * the user interface by providing a visual representation for each option.

 * Key features:
 * - Converts an array of drawable resource IDs to Drawable objects for consistent and easy icon management.
 * - Efficiently loads and displays text with icons using Android's View recycling mechanism.

 * Constructor:
 * - Accepts a context, an array of text items, and an array of icon resource IDs. It converts these IDs
 *   into Drawable objects for use in the list.

 * Method:
 * - `getView`: Inflates the custom layout for each list item, setting the appropriate text and icon
 *   for each position.
 */

public class IconTextAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] items;
    private final Drawable[] icons;

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