package nl.liacs.huecolor;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class IconListView extends ArrayAdapter<String> {
    private final Activity context;
    private final String[] content;
    private final Integer[] icons;

    public IconListView(Activity context, String[] content, Integer[] icons) {
        super(context, R.layout.list_item, content);
        this.context = context;
        this.content = content;
        this.icons = icons;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.list_item, null, true);
        TextView contentView = (TextView) rowView.findViewById(R.id.content);
        ImageView iconView = (ImageView) rowView.findViewById(R.id.icon);
        contentView.setText(content[position]);
        iconView.setImageResource(icons[position]);
        return rowView;
    }
}