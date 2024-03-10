package com.ChatBT_SIDD.PHCET.Bean;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.ChatBT_SIDD.PHCET.R;


public class ChatRightHolder extends RecyclerView.ViewHolder {
    private TextView tvContent;
    private TextView tvName;

    public ChatRightHolder(View itemView) {
        super(itemView);
        tvContent = (TextView) itemView.findViewById(R.id.tv_right);
        tvName = (TextView) itemView.findViewById(R.id.tv_device);
    }

    public TextView getTvContent() {
        return tvContent;
    }

    public void setTvContent(TextView tvContent) {
        this.tvContent = tvContent;
    }

    public TextView getTvName() {
        return tvName;
    }

    public void setTvName(TextView tvName) {
        this.tvName = tvName;
    }
}
