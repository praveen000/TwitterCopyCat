package com.example.android.twittercopycat.screens;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.twittercopycat.R;
import com.example.android.twittercopycat.application.TwitterCopyCatApplication;
import com.example.android.twittercopycat.fragments.MyTimelineFragment;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;

public class MyTimelineScreen extends AppCompatActivity {

    // Views
    private Button sendTweet;
    private EditText tweetText;
    private TextView numberOfCharacters;
    private ImageView shareBtn;
    private int tweetMaxCharacters;
    private TwitterCopyCatApplication app;
    private static final String TAG = "MyTimelineScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tweetMaxCharacters = getResources().getInteger(R.integer.max_characters);
        app = TwitterCopyCatApplication.getInstance();

        setContentView(R.layout.activity_my_timeline);

        // Hides keyboard to see list of tweets instead the keyboard when entering the screen
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //find views and adding listeners
        findViews();
        addViewListeners();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, MyTimelineFragment.newInstance(app.getUsername(), app.getPassword()))
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_my_timeline, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                menuRefresh();
                return true;
            case R.id.menu_settings:
                menuSettings();
                return true;
            case R.id.menu_logout:
                menuSignOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Menu Action Methods

    /**
     * Refresh action -  updates the list of recent tweets
     */
    private void menuRefresh(){
        MyTimelineFragment frag = (MyTimelineFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        if(app.isNetworkAvailable()){
            frag.updateTweets(!app.isNetworkAvailable());
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.cant_refresh),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Settings action - opens Settings activity
     */
    private void menuSettings(){
        Intent intent = new Intent(getApplicationContext(), SettingsScreen.class);
        startActivity(intent);
    }

    /**
     * LogOut action - logs out the application
     */
    private void menuSignOut(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        app.saveCredentials(false, null, null);

                        Intent intent = new Intent(getApplicationContext(), LoginScreen.class);
                        startActivity(intent);
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MyTimelineScreen.this);
        builder.setMessage(getString(R.string.are_you_sure)).setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener).show();
    }

    /**
     * Adds necessary listeners to every view
     */
    private void addViewListeners(){

        sendTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tweet = tweetText.getText().toString();

                if(checkValidTweet(tweet)) {
                    if(app.isNetworkAvailable()){
                        SendTweetTask tweetTask = new SendTweetTask(getApplicationContext());
                        tweetTask.execute(tweet);

                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                getString(R.string.unable_to_send_tweet),
                                Toast.LENGTH_LONG
                        ).show();

                        // Save the offline tweet to send it later
                        app.addOfflineTweet(tweet);

                        // Clear the text box
                        tweetText.setText("");
                    }
                }
            }
        });

        tweetText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                numberOfCharacters.setText(String.valueOf(tweetMaxCharacters - s.length()));
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String tweet = tweetText.getText().toString();

                if(checkValidTweet(tweet)) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, tweet);
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_to)));
                }
            }
        });
    }

    /**
     * Checks if tweet is valid
     * @param tweet string representing a tweet
     * @return true if tweet is valid to be sent or false if otherwise
     */
    private boolean checkValidTweet(String tweet){
        if(tweet.length() < 1 || tweet.length() > 140) {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.invalid_number_of_characters);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return false;
        }
        return true;
    }

    /**
     * Finds all views related to MyTimeline activity
     */
    private void findViews(){
        sendTweet = (Button) findViewById(R.id.button_send_tweet);
        tweetText = (EditText) findViewById(R.id.editText_tweet);
        numberOfCharacters = (TextView) findViewById(R.id.tweet_number_of_characters);
        shareBtn = (ImageView) findViewById(R.id.share_tweet);
    }

    /**
     * SendTweetTask
     *
     * Sends a new tweet
     */
    public class SendTweetTask extends AsyncTask<String, Void, Boolean> {

        private final String LOG_TAG = SendTweetTask.class.getSimpleName();
        private final String apiUrl = getString(R.string.api_url);
        private final Context mContext;

        public SendTweetTask(Context context) {
            mContext = context;
        }

        private boolean DEBUG = true;

        @Override
        protected Boolean doInBackground(String... tweet) {

            try{

                Twitter t = new Twitter(app.getUsername(), app.getPassword());
                t.setAPIRootUrl(apiUrl);
                t.updateStatus(tweet[0]);
                Log.d(LOG_TAG, "Sending this tweet: " + tweet);

            } catch (TwitterException e){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result) {
                // Sent successfully the tweet.  Hooray!
                Toast.makeText(mContext, getString(R.string.tweet_sent_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, getString(R.string.unable_to_send_tweet), Toast.LENGTH_LONG).show();
            }

            //clear the text box
            tweetText.setText("");
        }
    }
}
