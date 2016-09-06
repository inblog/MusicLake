package com.cyl.music_hnust.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyl.music_hnust.Json.JsonCallback;
import com.cyl.music_hnust.R;
import com.cyl.music_hnust.model.OnlineMusic;
import com.cyl.music_hnust.model.OnlineMusicList;
import com.cyl.music_hnust.model.OnlinePlaylistMusic;
import com.cyl.music_hnust.utils.Constants;
import com.cyl.music_hnust.utils.ImageUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

/**
 * 作者：yonglong on 2016/8/10 21:36
 * 邮箱：643872807@qq.com
 * 版本：2.5
 */
public class OnlineAdapter extends RecyclerView.Adapter<OnlineAdapter.ItemHolder> {

    private Context mContext;
    public List<OnlinePlaylistMusic> mData = new ArrayList<>();

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }
    public OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }


    public OnlineAdapter(Context context, RecyclerView mRecyclerView, List<OnlinePlaylistMusic> mData) {
        this.mContext = context;
        this.mData = mData;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_online, null);
        ItemHolder itemHolder = new ItemHolder(v);
        return itemHolder;

    }

    @Override
    public void onBindViewHolder(final ItemHolder holder, final int position) {

        holder.title.setText(mData.get(position).getTitle());
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(holder.itemView,position);
                }
            });
        }

//        holder.tv_1.setText(mData.get(position).getMusic1());
//        holder.tv_2.setText(mData.get(position).getMusic2());
//        holder.tv_3.setText(mData.get(position).getMusic3());
//        holder.iv_cover.setImageBitmap();
        getMusicListInfo(mData.get(position),holder);
    }

    @Override
    public int getItemCount() {
        return (null != mData ? mData.size() : 0);
    }

    public class ItemHolder extends RecyclerView.ViewHolder {
        protected TextView title, tv_1,tv_2,tv_3;
        protected ImageView iv_cover;

        public ItemHolder(View view) {
            super(view);
            this.iv_cover = (ImageView) view.findViewById(R.id.iv_cover);
            this.title = (TextView) view.findViewById(R.id.tv_music);
            this.tv_1 = (TextView) view.findViewById(R.id.tv_music_1);
            this.tv_2 = (TextView) view.findViewById(R.id.tv_music_2);
            this.tv_3 = (TextView) view.findViewById(R.id.tv_music_3);
        }

    }

    private void getMusicListInfo(final OnlinePlaylistMusic songListInfo, final ItemHolder holder) {
        if (songListInfo.getCoverUrl() == null) {
            holder.iv_cover.setTag(songListInfo.getTitle());
            holder.iv_cover.setImageResource(R.drawable.ic_account_circle_black_24dp);
            holder.tv_1.setText("1.加载中…");
            holder.tv_2.setText("2.加载中…");
            holder.tv_3.setText("3.加载中…");
            OkHttpUtils.get().url(Constants.BASE_URL)
                    .addParams(Constants.PARAM_METHOD, Constants.METHOD_GET_MUSIC_LIST)
                    .addParams(Constants.PARAM_TYPE, songListInfo.getType())
                    .addParams(Constants.PARAM_SIZE, "3")
                    .build()
                    .execute(new JsonCallback<OnlineMusicList>(OnlineMusicList.class) {
                        @Override
                        public void onResponse(OnlineMusicList response) {
                            if (response == null || response.getSong_list() == null) {
                                return;
                            }
                            if (!songListInfo.getTitle().equals(holder.iv_cover.getTag())) {
                                return;
                            }
                            parse(response, songListInfo);
                            setData(songListInfo, holder);
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                        }
                    });
        } else {
            //holder.ivCover.setTag(null);
            setData(songListInfo, holder);
        }
    }
    private void parse(OnlineMusicList response, OnlinePlaylistMusic songListInfo) {
        List<OnlineMusic> OnlineMusics = response.getSong_list();
        songListInfo.setCoverUrl(response.getBillboard().getPic_s260());
        if (OnlineMusics.size() >= 1) {
            songListInfo.setMusic1(mContext.getString(R.string.song_list_item_title_1,
                    OnlineMusics.get(0).getTitle(), OnlineMusics.get(0).getArtist_name()));
        } else {
            songListInfo.setMusic1("");
        }
        if (OnlineMusics.size() >= 2) {
            songListInfo.setMusic2(mContext.getString(R.string.song_list_item_title_2,
                    OnlineMusics.get(1).getTitle(), OnlineMusics.get(1).getArtist_name()));
        } else {
            songListInfo.setMusic2("");
        }
        if (OnlineMusics.size() >= 3) {
            songListInfo.setMusic3(mContext.getString(R.string.song_list_item_title_3,
                    OnlineMusics.get(2).getTitle(), OnlineMusics.get(2).getArtist_name()));
        } else {
            songListInfo.setMusic3("");
        }
    }
    private void setData(OnlinePlaylistMusic songListInfo, ItemHolder holder) {
        ImageLoader.getInstance().displayImage(songListInfo.getCoverUrl(), holder.iv_cover, ImageUtils.getCoverDisplayOptions());
        holder.tv_1.setText(songListInfo.getMusic1());
        holder.tv_2.setText(songListInfo.getMusic2());
        holder.tv_3.setText(songListInfo.getMusic3());
    }


}