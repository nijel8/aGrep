package bg.nijel.aGrep.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import bg.nijel.aGrep.Prefs;
import bg.nijel.aGrep.R;

/**
 * Created by Nick on 1/6/2016.
 */
public class AutoComleteDropDownAdapter<T> extends ArrayAdapter<T> {

    public AutoComleteDropDownAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, 0, objects);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ListView list = (ListView) parent;
        if (list != null) {
            list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> av, View v, final int pos, long id) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(getItem(pos).toString())
                            .setMessage(getContext().getString(R.string.label_remove_item))
                            .setPositiveButton(R.string.label_OK, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Prefs.removeRecentItem(getContext(), getItem(pos).toString());
                                    remove(getItem(pos));
                                    notifyDataSetChanged();
                                }
                            })
                            .setNegativeButton(R.string.label_CANCEL, null)
                            .setCancelable(true)
                            .show();
                    return false;
                }
            });
        }
        return super.getView(position, convertView, parent);
    }
}