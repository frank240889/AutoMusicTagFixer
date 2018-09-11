package mx.dev.franco.automusictagfixer.UI.search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.track_detail.TrackDetailsActivity;
import mx.dev.franco.automusictagfixer.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class SearchActivity extends AppCompatActivity implements AsyncSearch.ResultsSearchListener, FoundItemHolder.ClickListener {
    private static final String TAG = SearchActivity.class.getName();
    public static final int UPDATE_ITEM_ON_RETURN = 0;
    private static AsyncSearch mAsyncSearch;
    @Inject
    TrackRepository mTrackRepository;
    //A simple texview to show a message when no songs were identificationFound
    private TextView mMessage;
    //recycler view is a component that delivers
    //better performance with huge data sources
    private RecyclerView mRecyclerView;
    private SearchTrackAdapter mAdapter;
    private Toolbar mToolbar;
    private ActionBar mActionBar;
    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.activity_search);
        AutoMusicTagFixer.getContextComponent().inject(this);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        //pressing back from toolbar, close activity
        mToolbar.setNavigationOnClickListener(v -> finish());

        //attach adapter to our recyclerview
        mRecyclerView = findViewById(R.id.found_tracks_recycler_view);
        mMessage = findViewById(R.id.found_message);


        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.SPACE_BETWEEN);
        layoutManager.setAlignItems(AlignItems.CENTER);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new SearchTrackAdapter(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isDestroyed()) {
                    Glide.with(SearchActivity.this).resumeRequests();
                }
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING && !isDestroyed()) {
                    Glide.with(SearchActivity.this).pauseRequests();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        performSearch(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        performSearch(intent);
    }

    private void performSearch(Intent intent){
        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            String q = "%"+intent.getStringExtra(SearchManager.QUERY)+"%";
            if(mAsyncSearch == null){
                mAsyncSearch = new AsyncSearch(this, mTrackRepository);
                mAsyncSearch.execute(q);
            }
        }
    }

    @Override
    public boolean onSearchRequested() {
        return true;
    }


    @Override
    public void onStartSearch() {
        mRecyclerView.stopScroll();
        mAdapter.reset();
    }

    @SuppressLint({"StringFormatMatches", "StringFormatInvalid"})
    @Override
    public void onFinished(List<Track> results) {
        if(results != null) {
            if(results.size() > 0) {
                mMessage.setVisibility(View.GONE);
                mActionBar.setTitle( String.format(getString(R.string.search_results),results.size()+"",mQuery) );
            }
            else {
                mActionBar.setTitle( String.format(getString(R.string.no_found_items),mQuery) );
                mMessage.setVisibility(View.VISIBLE);
            }
            mAdapter.swapData(results);
        }

        mAsyncSearch = null;
        mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onCancelled() {
        mAsyncSearch = null;
    }

    @Override
    public void onItemClick(int position, View view) {
        mRecyclerView.stopScroll();
        Track track = mAdapter.getDatasource().get(position);
        if(track.processing() == 0) {
            Intent intent = new Intent(this, TrackDetailsActivity.class);
            intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
            intent.putExtra(Constants.CorrectionModes.MODE, Constants.CorrectionModes.SEMI_AUTOMATIC);
            startActivityForResult(intent, UPDATE_ITEM_ON_RETURN);
        }
        else {
            Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, this);
            snackbar.setText(R.string.current_file_processing);
            snackbar.show();
        }
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == Activity.RESULT_OK && data != null){
            mAdapter.updateTrack(data);
        }
    }



    @Override
    public void onDestroy(){
        super.onDestroy();
        mRecyclerView.stopScroll();
        if(mAsyncSearch != null && (mAsyncSearch.getStatus() == AsyncSearch.Status.PENDING || mAsyncSearch.getStatus() == AsyncSearch.Status.RUNNING)){
            mAsyncSearch.cancel(true);
        }
        mAdapter.destroy();
        mAsyncSearch = null;
        mTrackRepository = null;
        mMessage = null;
        mRecyclerView = null;
        mAdapter = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search_activity, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        return true;
    }
}
