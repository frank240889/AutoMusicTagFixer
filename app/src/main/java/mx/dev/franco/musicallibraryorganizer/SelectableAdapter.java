package mx.dev.franco.musicallibraryorganizer;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by franco on 25/07/17.
 */

public abstract class SelectableAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    @SuppressWarnings("unused")
    private static final String TAG = SelectableAdapter.class.getSimpleName();

    private SparseBooleanArray selectedItems;
    private SparseBooleanArray idsSelectedItems;

    public SelectableAdapter() {
        selectedItems = new SparseBooleanArray();
        idsSelectedItems = new SparseBooleanArray();
    }

    /**
     * Indicates if the item at position position is selected
     * @param position Position of the item to check
     * @return true if the item is selected, false otherwise
     */
    public boolean isSelected(int position) {
        return getSelectedItems().contains(position);
    }

    /**
     * Toggle the selection status of the item at a given position
     * @param position Position of the item to toggle the selection status for
     */
    public void toggleSelection(int position, long id) {
        Log.d("toggle_position",position+"");
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
            idsSelectedItems.delete((int)id);
        } else {
            selectedItems.put(position, true);
            idsSelectedItems.put((int)id, true);
        }

        notifyItemChanged(position);
    }

    /**
     * Clear the selection status for all items
     */
    public void clearSelection() {
        List<Integer> selection = getSelectedItems();
        selectedItems.clear();
        idsSelectedItems.clear();
        for (Integer i : selection) {
            notifyItemChanged(i);
        }
    }

    /**
     * Count the selected items
     * @return Selected items count
     */
    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    /**
     * Indicates the list of selected items
     * @return List of selected items ids
     */
    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); ++i) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public String[] getIdsSelectedItems() {
        int size = idsSelectedItems.size();
        String[] ids = new String[size];
        for (int i = 0; i < size; i++) {
            ids[i] = idsSelectedItems.keyAt(i)+"";
        }
        return ids;
    }
}
