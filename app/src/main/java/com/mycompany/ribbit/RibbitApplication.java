package com.mycompany.ribbit;

import android.app.Application;

import com.mycompany.ribbit.ui.MainActivity;
import com.mycompany.ribbit.utils.ParseConstant;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.PushService;


public class RibbitApplication extends Application {

    @Override
    public void onCreate(){
        super.onCreate();
        Parse.initialize(this, "1vgPL41oaHdcaDvg7GCgEbh20sTYpri7acEwHJqg", "HoEudt4zOexCWWDPqKeyo3AfljcK7ovNQ2gahnSm");
    }
    public static void updateParseInstallation(ParseUser user){
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put(ParseConstant.KEY_USER_ID, user.getObjectId());
        installation.saveInBackground();
    }
}
