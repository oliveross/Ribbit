package com.mycompany.ribbit.adapters;


import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycompany.ribbit.R;
import com.mycompany.ribbit.utils.MD5Util;
import com.mycompany.ribbit.utils.ParseConstant;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.List;

public class UserAdapter extends ArrayAdapter<ParseUser> {



    protected Context mContext;
    protected List<ParseUser> mUsers;

    public UserAdapter(Context context, List<ParseUser> users){
        super(context, R.layout.message_item, users);
        mContext = context;
        mUsers = users;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        //a new view is created for the first time and then recycled for efficiency
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.user_item, null);
            holder = new ViewHolder();
            holder.userImageView = (ImageView) convertView.findViewById(R.id.userImageView);
            holder.checkImageView = (ImageView) convertView.findViewById(R.id.checkImageView);
            holder.nameLabel = (TextView) convertView.findViewById(R.id.nameLabel);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }

        ParseUser user = mUsers.get(position);
        String email = user.getEmail().toLowerCase();

        if(email.equals("")){
            holder.userImageView.setImageResource(R.drawable.avatar_empty);
        }else{
            String hash = MD5Util.md5Hex(email);
            String gravatarUrl = "http://www.gravatar.com/avatar/" + hash + "?s=204&d=404";
            Picasso.with(mContext)
                    .load(gravatarUrl)
                    .placeholder(R.drawable.avatar_empty)
                    .into(holder.userImageView);
        }

        //setting the icon base on the file type
        /*if(user.getString(ParseConstant.KEY_FILE_TYPE).equals(ParseConstant.TYPE_IMAGE)){
            holder.iconImageView.setImageResource(R.drawable.ic_picture);
        }else{
            holder.iconImageView.setImageResource(R.drawable.ic_video);
        }*/

        //setting the name label of the message
        holder.nameLabel.setText(user.getUsername());

        //setting the checkmark image overlay on user image when item is selected in the grid view
        GridView gridView = (GridView) parent;
        if(gridView.isItemChecked(position)){
            holder.checkImageView.setVisibility(View.VISIBLE);
        }else{
            holder.checkImageView.setVisibility(View.INVISIBLE);
        }
            return convertView;
    }

    private static class ViewHolder{
        ImageView userImageView;
        ImageView checkImageView;
        TextView nameLabel;
    }

    public void refill(List<ParseUser> users){
        mUsers.clear();
        mUsers.addAll(users);
        notifyDataSetChanged();
    }
}
