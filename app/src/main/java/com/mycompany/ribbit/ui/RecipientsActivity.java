package com.mycompany.ribbit.ui;

import android.app.AlertDialog;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
import android.widget.Toast;

import com.mycompany.ribbit.adapters.UserAdapter;
import com.mycompany.ribbit.utils.FileHelper;
import com.mycompany.ribbit.utils.ParseConstant;
import com.mycompany.ribbit.R;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class RecipientsActivity extends ActionBarActivity {

    public static final String TAG = RecipientsActivity.class.getSimpleName();

    protected ParseRelation<ParseUser> mFriendsRelation;
    protected ParseUser mCurrentUser;
    protected List<ParseUser> mFriends;
    protected MenuItem mSendMenuItem;
    protected Uri mMediaUri;
    protected String mFileType;

    @InjectView(R.id.friendsGrid) GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_grid);
        ButterKnife.inject(this);



        mGridView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mGridView.setOnItemClickListener(mOnItemClickListener);

        //getting the Uri and file type of the file
        mMediaUri = getIntent().getData();
        mFileType = getIntent().getExtras().getString(ParseConstant.KEY_FILE_TYPE);

        TextView emptyTextView = (TextView)findViewById(android.R.id.empty);
        mGridView.setEmptyView(emptyTextView);

    }

    @Override
    public void onResume() {
        super.onResume();

        mCurrentUser = ParseUser.getCurrentUser();
        mFriendsRelation = mCurrentUser.getRelation(ParseConstant.KEY_FRIENDS_RELATION);



        ParseQuery<ParseUser> query = mFriendsRelation.getQuery();
        query.addAscendingOrder(ParseConstant.KEY_USERNAME);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e == null) {
                    mFriends = parseUsers;

                    String[] usernames = new String[mFriends.size()];
                    int i = 0;
                    for (ParseUser user : mFriends) {
                        usernames[i] = user.getUsername();
                        i++;
                    }
                    if(mGridView.getAdapter() == null) {
                        UserAdapter adapter = new UserAdapter(RecipientsActivity.this, mFriends);
                        mGridView.setAdapter(adapter);
                    }else{
                        ((UserAdapter)mGridView.getAdapter()).refill(mFriends);
                    }
                } else {
                    Log.e(TAG, e.getMessage());

                    AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recipients, menu);
        mSendMenuItem = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send) {
            //A ParseObject named message is created to store
            //the information for the message to be sent
            ParseObject message = createMessage();
            if(message == null){
                //error
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.error_selecting_file))
                        .setTitle(getString(R.string.error_selecting_file_title))
                        .setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }else{
                send(message);
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void send(ParseObject message) {

        //message is sent to user by saving in backend
        message.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null){
                    //success!
                    Toast.makeText(RecipientsActivity.this, getString(R.string.success_message), Toast.LENGTH_SHORT).show();
                    sendPushNotification();
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
                    builder.setMessage(getString(R.string.error_sending_message))
                            .setTitle(getString(R.string.error_title))
                                    .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });
    }

    protected ParseObject createMessage(){

        //constructor for the message
        ParseObject message = new ParseObject(ParseConstant.CLASS_MESSAGE);
        message.put(ParseConstant.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        message.put(ParseConstant.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());
        message.put(ParseConstant.KEY_RECIPIENT_IDS, getRecipientIds());
        message.put(ParseConstant.KEY_FILE_TYPE, mFileType);

        //FileHelper converts the image or video to a byte array
        byte[] fileBytes = FileHelper.getByteArrayFromFile(this, mMediaUri);

        if(fileBytes == null){
            return null;
        }else{
            //if it is an image the size of the file is reduced
            if(mFileType.equals(ParseConstant.TYPE_IMAGE)){
                fileBytes = FileHelper.reduceImageForUpload(fileBytes);
            }

            String fileName = FileHelper.getFileName(this, mMediaUri, mFileType);

            //the byte array is used to create a ParseFile variable
            //and is added to the message
            ParseFile file = new ParseFile(fileName, fileBytes);
            message.put(ParseConstant.KEY_FILE, file);

            return message;
        }
    }

    protected ArrayList<String> getRecipientIds(){

        //getting recipient ids from the listview
        ArrayList<String> recipientIds = new ArrayList<String>();
        for(int i = 0; i < mGridView.getCount(); i++){
            if(mGridView.isItemChecked(i)){
                recipientIds.add(mFriends.get(i).getObjectId());
            }
        }
        return recipientIds;
    }

    protected AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // send button is only visible when a recipient is checked from the list
            if(mGridView.getCheckedItemCount() > 0){
                mSendMenuItem.setVisible(true);
            }else{
                mSendMenuItem.setVisible(false);
            }

            ImageView checkImageView = (ImageView)view.findViewById(R.id.checkImageView);
            if(mGridView.isItemChecked(position)){
                //add the recipients
                checkImageView.setVisibility(View.VISIBLE);
            }else{
                //remove the recipients
                checkImageView.setVisibility(View.INVISIBLE);
            }
        }
    };

    protected void sendPushNotification(){
        ParseQuery<ParseInstallation> query = ParseInstallation.getQuery();
        query.whereContainedIn(ParseConstant.KEY_USER_ID, getRecipientIds());

        //send push notification
        ParsePush push = new ParsePush();
        push.setQuery(query);
        push.setMessage(getString(R.string.push_message, ParseUser.getCurrentUser().getUsername()));
        push.sendInBackground();
    }
}
