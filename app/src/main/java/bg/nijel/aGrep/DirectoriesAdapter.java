package bg.nijel.aGrep;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Nick on 1/9/2016.
 */
public class DirectoriesAdapter extends RecyclerView.Adapter<DirectoriesAdapter.DirectoryViewHolder> {

    private ArrayList<CheckedString> dirList;

    public DirectoriesAdapter(ArrayList<CheckedString> dirs) {
        this.dirList = dirs;
    }

    @Override
    public int getItemCount() {
        return dirList.size();
    }

    public void setAllChecked(boolean checked) {
        for (int i = 0; i < getItemCount(); i++){
            dirList.set(i, dirList.get(i).setChecked(checked));
        }
        notifyDataSetChanged();
        Settings.sortSaveDirsList(null, dirList);
    }

    @Override
    public void onBindViewHolder(DirectoryViewHolder dirViewHolder, final int i) {
        final CheckedString dir = dirList.get(i);
        dirViewHolder.dirName.setText(dir.string.substring(dir.string.lastIndexOf("/")+1, dir.string.length()));
        dirViewHolder.dirParent.setText(dir.string.substring(0, dir.string.lastIndexOf("/") + 1));
        dirViewHolder.dirName.setOnCheckedChangeListener(null);
        dirViewHolder.dirName.setChecked(dir.checked);
        dirViewHolder.dirName.setTag(dir);
        dirViewHolder.dirName.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (i < getItemCount()) {
                    dirList.set(i, dir.setChecked(isChecked));
                   // RootShell.log("dirList: " + dirList.size());
                    Settings.sortSaveDirsList(dir, dirList);
                }
            }
        });
        dirViewHolder.openFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(dir.string);
                intent.setDataAndType(uri, "resource/folder");
                if (intent.resolveActivityInfo(v.getContext().getPackageManager(), 0) != null) {
                    v.getContext().startActivity(Intent.createChooser(intent, "Open folder"));
                } else {
                    Toast.makeText(v.getContext(), v.getContext().getString(R.string.no_file_manager_found), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public DirectoryViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.cardview_dirs, viewGroup, false);
        return new DirectoryViewHolder(itemView);
    }

    @Override
    public void onAttachedToRecyclerView( RecyclerView recyclerView) {
    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            int pos = viewHolder.getAdapterPosition();
            dirList.remove(pos);
            notifyItemRemoved(pos);
            Settings.sortSaveDirsList(null, dirList);
        }
    };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public static class DirectoryViewHolder extends RecyclerView.ViewHolder {

        CheckBox dirName;
        TextView dirParent;
        ImageButton openFolder;


        public DirectoryViewHolder(View v) {
            super(v);
            dirName =  (CheckBox) v.findViewById(R.id.dir_name);
            dirParent = (TextView) v.findViewById(R.id.dir_parent);
            openFolder = (ImageButton) v.findViewById(R.id.ButtonOpenFolder);
        }
    }
}
