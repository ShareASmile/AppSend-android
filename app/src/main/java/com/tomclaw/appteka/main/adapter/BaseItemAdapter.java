package com.tomclaw.appteka.main.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tomclaw.appteka.R;
import com.tomclaw.appteka.main.adapter.holder.AbstractItemHolder;
import com.tomclaw.appteka.main.adapter.holder.AppItemHolder;
import com.tomclaw.appteka.main.adapter.holder.CouchItemHolder;
import com.tomclaw.appteka.main.adapter.holder.DonateItemHolder;
import com.tomclaw.appteka.main.item.BaseItem;

import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.appteka.main.item.BaseItem.APP_ITEM;
import static com.tomclaw.appteka.main.item.BaseItem.COUCH_ITEM;
import static com.tomclaw.appteka.main.item.BaseItem.DONATE_ITEM;

/**
 * Created by Solkin on 10.12.2014.
 */
public class BaseItemAdapter extends RecyclerView.Adapter<AbstractItemHolder> {

    private final List<BaseItem> itemsList;
    private LayoutInflater inflater;
    private Context context;

    private BaseItemClickListener listener;

    public BaseItemAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.itemsList = new ArrayList<>();
    }

    public void setListener(BaseItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public AbstractItemHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case APP_ITEM:
                view = inflater.inflate(R.layout.app_item, viewGroup, false);
                return new AppItemHolder(view);
            case DONATE_ITEM:
                view = inflater.inflate(R.layout.donate_item, viewGroup, false);
                return new DonateItemHolder(view);
            case COUCH_ITEM:
                view = inflater.inflate(R.layout.couch_item, viewGroup, false);
                return new CouchItemHolder(view);
            default:
                throw new IllegalStateException("Unsupported item type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(AbstractItemHolder holder, int position) {
        BaseItem appInfo = itemsList.get(position);
        boolean isLast = (itemsList.size() - 1 == position);
        holder.bind(context, appInfo, isLast, listener);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return itemsList.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    public void setItemsList(List<? extends BaseItem> items) {
        itemsList.clear();
        itemsList.addAll(items);
    }

    public List<? extends BaseItem> getItemsList() {
        return itemsList;
    }

    public void clearItemsList() {
        itemsList.clear();
    }

    public void addToItemsList(List<BaseItem> items) {
        itemsList.addAll(items);
    }

    public interface BaseItemClickListener<I extends BaseItem> {
        void onItemClicked(I item);

        void onActionClicked(I item, String action);
    }
}
