package mx.dev.franco.automusictagfixer.ui.main;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class DiffResults<T> {
    public List<T> list;
    public DiffUtil.DiffResult diffResult;
}
