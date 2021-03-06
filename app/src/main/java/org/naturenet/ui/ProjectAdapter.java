package org.naturenet.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Project;

import java.util.List;

import timber.log.Timber;

public class ProjectAdapter extends ArrayAdapter<Project> implements View.OnClickListener {

    public ProjectAdapter(Context context, List<Project> objects) {
        super(context, R.layout.project_list_item, objects);
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = convertView;

        if (convertView == null) {
            view = inflater.inflate(R.layout.project_list_item, parent, false);
        }

        Project project = getItem(position);
        view.setTag(project);
        ImageView thumbnail = (ImageView) view.findViewById(R.id.project_thumbnail);
        Picasso.with(getContext()).load(Strings.emptyToNull(project.iconUrl)).fit().into(thumbnail);
        TextView name = (TextView) view.findViewById(R.id.project_name);
        name.setText(project.name);

        return view;
    }

    @Override
    public void onClick(View v) {
        Timber.d("Clicked on project %s", v.getTag().toString());
    }
}