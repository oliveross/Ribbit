package com.mycompany.ribbit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mycompany.ribbit.adapters.UserAdapter;
import com.mycompany.ribbit.utils.ParseConstant;
import com.mycompany.ribbit.R;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class EditFriendsActivity extends ActionBarActivity {
    // some random comment

    public static final String TAG = EditFriendsActivity.class.getSimpleName();

    public List<ParseUser> mUsers;
    protected ParseRelation<ParseUser> mFriendsRelation;
    protected ParseUser mCurrentUser;

    @InjectView(R.id.friendsGrid) GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_grid);
        ButterKnife.inject(this);

        TextView emptyTextView = (TextView)findViewById(android.R.id.empty);
        mGridView.setEmptyView(emptyTextView);

        mGridView.setOnItemClickListener(mOnItemClickListener);

        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);
    }


    @Override
    protected void onResume() {
        super.onResume();

        mCurrentUser = ParseUser.getCurrentUser();
        mFriendsRelation = mCurrentUser.getRelation(ParseConstant.KEY_FRIENDS_RELATION);

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.orderByAscending(ParseConstant.KEY_USERNAME);
        query.setLimit(1000);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if(e == null){
                    mUsers = parseUsers;
                    String[] usernames = new String[mUsers.size()];
                    int i = 0;
                    for(ParseUser parseUser : mUsers){
                        usernames[i] = parseUser.getUsername();
                        i++;
                    }
                    if(mGridView.getAdapter() == null) {
                        UserAdapter adapter = new UserAdapter(EditFriendsActivity.this, mUsers);
                        mGridView.setAdapter(adapter);
                    }else{
                        ((UserAdapter)mGridView.getAdapter()).refill(mUsers);
                    }

                    addFriendCheckmarks();
                }else{
                    Log.e(TAG, e.getMessage());

                    AlertDialog.Builder builder = new AlertDialog.Builder(EditFriendsActivity.this);
                    builder.setMessage(e.getMessage())
                            .setTitle(R.string.error_title)
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addFriendCheckmarks() {
        mFriendsRelation.getQuery().findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if(e == null){
                    for(int i = 0; i < mUsers.size(); i++){
                        ParseUser user = mUsers.get(i);

                        for(ParseUser friend : parseUsers){
                            if(friend.getObjectId().equals(user.getObjectId())){
                                mGridView.setItemChecked(i, true);
                            }
                        }
                    }
                }else{
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }


//    @Override
//    protected void onListItemClick(ListView l, View v, int position, long id) {
//        super.onListItemClick(l, v, position, id);
//
//        if(getListView().isItemChecked(position)){
//            mFriendsRelation.add(mUsers.get(position));
//        }else{
//            mFriendsRelation.remove(mUsers.get(position));
//        }
//        mCurrentUser.saveInBackground(new SaveCallback() {
//            @Override
//            public void done(ParseException e) {
//                if (e != null){
//                    Log.e(TAG, e.getMessage());
//                }
//            }
//        });
//    }

    protected AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,long id){
                  ImageView checkImageView = (ImageView)view.findViewById(R.id.checkImageView);

            if(mGridView.isItemChecked(position)){
                //add friends
                mFriendsRelation.add(mUsers.get(position));
                checkImageView.setVisibility(View.VISIBLE);
            }else{
                //remove friends
                mFriendsRelation.remove(mUsers.get(position));
                checkImageView.setVisibility(View.INVISIBLE);
            }

            mCurrentUser.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if(e != null){
                        Log.e(TAG, e.getMessage());
                    }
                }
            });
        }
    };
}

