package com.gmail.jorgegilcavazos.ballislife.features.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.gmail.jorgegilcavazos.ballislife.BuildConfig;
import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger;
import com.gmail.jorgegilcavazos.ballislife.analytics.GoPremiumOrigin;
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishEvent;
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishEventParam;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService;
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.data.repository.posts.PostsRepository;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesHomeFragment;
import com.gmail.jorgegilcavazos.ballislife.features.gopremium.GoPremiumActivity;
import com.gmail.jorgegilcavazos.ballislife.features.highlights.HighlightsMenuFragment;
import com.gmail.jorgegilcavazos.ballislife.features.login.LoginActivity;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishTheme;
import com.gmail.jorgegilcavazos.ballislife.features.playoffs.bracket.BracketFragment;
import com.gmail.jorgegilcavazos.ballislife.features.posts.PostsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.profile.ProfileActivity;
import com.gmail.jorgegilcavazos.ballislife.features.settings.SettingsActivity;
import com.gmail.jorgegilcavazos.ballislife.features.settings.SettingsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.standings.StandingsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.tour.TourLoginActivity;
import com.gmail.jorgegilcavazos.ballislife.util.ActivityUtils;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;
import com.gmail.jorgegilcavazos.ballislife.util.StringUtils;
import com.gmail.jorgegilcavazos.ballislife.util.UnitUtils;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.kobakei.ratethisapp.RateThisApp;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import jonathanfinerty.once.Once;

public class MainActivity extends BaseNoActionBarActivity {
    private static final String SHOW_TOUR = "showTourTag";
    private static final String SHOW_WHATS_NEW = "showWhatsNew";

    private static final String SELECTED_FRAGMENT_KEY = "selectedFragment";
    private static final String SELECTED_SUBREDDIT_KEY = "selectedSubreddit";

    private static final int GAMES_FRAGMENT_ID = 1;
    private static final int STANDINGS_FRAGMENT_ID = 2;
    private static final int POSTS_FRAGMENT_ID = 3;
    private static final int HIGHLIGHTS_FRAGMENT_ID = 4;
    private static final int PLAYOFF_FRAGMENT_ID = 5;

    // Shortcuts should match values in xml/shortcuts
    private static final String SHORTCUT_KEY = "shortcut";
    private static final String SHORTCUT_RNBA = "shortcut_rnba";
    private static final String SHORTCUT_HIGHLIGHTS = "shortcut_highlights";

    // Dynamic shortcut
    private static final String TEAM_SUB_SHORTCUT_ID = "teamSubShortcut";
    private static final String SHORTCUT_TEAM_SUB = "shortcut_teamsub";

    // Should match value in strings.xml
    private static final String NO_FAV_TEAM_VAL = "noteam";

    @Inject PremiumService premiumService;
    @Inject LocalRepository localRepository;
    @Inject @Named("redditSharedPreferences") SharedPreferences redditSharedPrefs;
    @Inject PostsRepository postsRepository;
    @Inject RedditAuthentication redditAuthentication;
    @Inject BaseSchedulerProvider schedulerProvider;
    @Inject EventLogger eventLogger;

    @BindView(R.id.mainAppBarLayout) AppBarLayout appBarLayout;

    Toolbar toolbar;
    ActionBar actionBar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    int selectedFragment;
    String subreddit;

    private FirebaseAnalytics firebaseAnalytics;
    private CompositeDisposable disposables;

    @Override
    public void injectAppComponent() {
        BallIsLifeApplication.getAppComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Fabric.with(this, new Crashlytics());
        MobileAds.initialize(this, "ca-app-pub-1607327298064379~6693958953");

        setupRemoteConfig();

        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener(task -> {
               if (task.isSuccessful()) {
                   Log.d(MainActivity.class.getSimpleName(), "FirebaseAuth sign in successful");
               } else {
                   Log.d(MainActivity.class.getSimpleName(), "FirebaseAuth sign in unsuccessful");
               }
            });

        // Show app tour if first install.
        if (!Once.beenDone(Once.THIS_APP_INSTALL, SHOW_TOUR)) {
            Intent intent = new Intent(this, TourLoginActivity.class);
            startActivity(intent);
            Once.markDone(SHOW_TOUR);
        } else {
            if (!Once.beenDone(Once.THIS_APP_VERSION, SHOW_WHATS_NEW)
                    && localRepository.shouldShowWhatsNew()) {
                new MaterialDialog.Builder(this).title(R.string.whats_new)
                        .content(R.string.whats_new_content)
                        .positiveText(R.string.got_it)
                        .negativeText(R.string.dont_show_again)
                        .onNegative((d, w) -> localRepository.setShouldShowWhatsNew(false))
                        .show();
                Once.markDone(SHOW_WHATS_NEW);
            }

            // Monitor launch times and interval from installation
            RateThisApp.onCreate(this);
            // If the condition is satisfied, "Rate this app" dialog will be shown
            RateThisApp.showRateDialogIfNeeded(this);
        }

        setUpToolbar();
        setUpNavigationView();
        setUpDrawerContent();
        loadRedditUsername();
        loadThemeToggleIcon();
        setupDynamicShortcut();

        // TODO: Move this out of here, either to application start or a presenter.
        disposables = new CompositeDisposable();

        disposables.add(premiumService.isPremiumUpdates()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(this::setGoPremiumVisibility));

        disposables.add(redditAuthentication.authenticate()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        loadRedditUsername();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }));

        // Set default fragment to selected startup page from preferences.
        switch (localRepository.getStartupFragment()) {
            case SettingsFragment.STARTUP_FRAGMENT_GAMES:
                selectedFragment = GAMES_FRAGMENT_ID;
                break;
            case SettingsFragment.STARTUP_FRAGMENT_RNBA:
                selectedFragment = POSTS_FRAGMENT_ID;
                break;
            case SettingsFragment.STARTUP_FRAGMENT_HIGHLIGHTS:
                selectedFragment = HIGHLIGHTS_FRAGMENT_ID;
                break;
            default:
                throw new IllegalStateException("Invalid startup fragment: " + localRepository
                        .getStartupFragment());
        }

        // Default posts fragment subreddit is r/nba
        subreddit = "nba";

        if (savedInstanceState != null) {
            // Restore fragment and selected subreddit if in posts fragment.
            selectedFragment = savedInstanceState.getInt(SELECTED_FRAGMENT_KEY);
            subreddit = savedInstanceState.getString(SELECTED_SUBREDDIT_KEY);
        } else {
            // No saved instance, we are either starting up the Activity from the launcher of from
            // a shortcut.
            if (getIntent().getExtras() != null) {
                String shortcut = getIntent().getStringExtra(SHORTCUT_KEY);
                if (shortcut != null) {
                    // Setup opening fragment if app opened from shortcut.
                    if (shortcut.equals(SHORTCUT_RNBA)) {
                        subreddit = "nba";
                        selectedFragment = POSTS_FRAGMENT_ID;
                    } else if (shortcut.equals(SHORTCUT_HIGHLIGHTS)) {
                        selectedFragment = HIGHLIGHTS_FRAGMENT_ID;
                    } else if (shortcut.equals(SHORTCUT_TEAM_SUB)) {
                        // Team shortcut opened, find favorite sub and set sub and fragment to open.
                        String teamSub = getTeamSubFromFavoritePref();
                        if (teamSub != null) {
                            subreddit = teamSub;
                            selectedFragment = POSTS_FRAGMENT_ID;
                        }
                    }
                }
            }
        }

        switch (selectedFragment) {
            case GAMES_FRAGMENT_ID:
                setGamesFragment();
                break;
            case STANDINGS_FRAGMENT_ID:
                setStandingsFragment();
                break;
            case POSTS_FRAGMENT_ID:
                setPostsFragment(subreddit);
                break;
            case HIGHLIGHTS_FRAGMENT_ID:
                setHighlightsFragment();
                break;
            case PLAYOFF_FRAGMENT_ID:
                setPlayoffsFragment();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_FRAGMENT_KEY, selectedFragment);
        outState.putString(SELECTED_SUBREDDIT_KEY, subreddit);

        super.onSaveInstanceState(outState);
    }

    private void setUpToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            actionBar = getSupportActionBar();
            if (actionBar != null) {
                // Show menu icon
                actionBar.setHomeAsUpIndicator(R.mipmap.ic_menu_white);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            setToolbarPopupTheme(toolbar);
        }
    }

    private void setUpNavigationView() {
        if (toolbar != null) {
            drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            navigationView = (NavigationView) findViewById(R.id.navigation);
            if (navigationView != null) {
                NavigationMenuView navMenuView = (NavigationMenuView) navigationView.getChildAt(0);
                if (navMenuView != null) {
                    navMenuView.setVerticalScrollBarEnabled(false);
                }
                updateNavViewFavoriteTeam();
                setGoPremiumVisibility(premiumService.isPremium());
            }
        }
    }

    private void setUpDrawerContent() {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            menuItem.setChecked(true);
            switch (menuItem.getItemId()) {
                case R.id.navigation_item_1:
                    setGamesFragment();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.navigation_item_2:
                    setStandingsFragment();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.navigation_item_3:
                    setPostsFragment("NBA");
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.navigation_item_4:
                    setHighlightsFragment();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.navigation_item_5:
                    setPlayoffsFragment();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.navigation_item_9:
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity
                            .class);
                    startActivity(settingsIntent);
                    return true;
                case R.id.nav_atl:
                    setPostsFragment(Constants.SUB_ATL);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_bkn:
                    setPostsFragment(Constants.SUB_BKN);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_bos:
                    setPostsFragment(Constants.SUB_BOS);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_cha:
                    setPostsFragment(Constants.SUB_CHA);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_chi:
                    setPostsFragment(Constants.SUB_CHI);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_cle:
                    setPostsFragment(Constants.SUB_CLE);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_dal:
                    setPostsFragment(Constants.SUB_DAL);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_den:
                    setPostsFragment(Constants.SUB_DEN);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_det:
                    setPostsFragment(Constants.SUB_DET);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_gsw:
                    setPostsFragment(Constants.SUB_GSW);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_hou:
                    setPostsFragment(Constants.SUB_HOU);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_ind:
                    setPostsFragment(Constants.SUB_IND);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_lac:
                    setPostsFragment(Constants.SUB_LAC);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_lal:
                    setPostsFragment(Constants.SUB_LAL);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_mem:
                    setPostsFragment(Constants.SUB_MEM);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_mia:
                    setPostsFragment(Constants.SUB_MIA);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_mil:
                    setPostsFragment(Constants.SUB_MIL);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_min:
                    setPostsFragment(Constants.SUB_MIN);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_nop:
                    setPostsFragment(Constants.SUB_NOP);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_nyk:
                    setPostsFragment(Constants.SUB_NYK);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_okc:
                    setPostsFragment(Constants.SUB_OKC);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_orl:
                    setPostsFragment(Constants.SUB_ORL);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_phi:
                    setPostsFragment(Constants.SUB_PHI);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_phx:
                    setPostsFragment(Constants.SUB_PHO);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_por:
                    setPostsFragment(Constants.SUB_POR);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_sac:
                    setPostsFragment(Constants.SUB_SAC);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_sas:
                    setPostsFragment(Constants.SUB_SAS);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_tor:
                    setPostsFragment(Constants.SUB_TOR);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_uta:
                    setPostsFragment(Constants.SUB_UTA);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                case R.id.nav_was:
                    setPostsFragment(Constants.SUB_WAS);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                /*
                    Disabled until https://github.com/mattbdean/JRAW/pull/167 is included in the
                    JRAW release.

                case R.id.nav_swish_multi:
                    setPostsFragment(Constants.MULTI_SWISH);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;

                 */
                case R.id.go_premium:
                    onGoPremiumClick();
                    return true;
                default:
                    setGamesFragment();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
            }
        });
    }

    public void setGamesFragment() {
        setTitle("Games");
        getSupportActionBar().setSubtitle(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setElevation(0);
        }

        // Use new fragment instance so that the viewpager / dates are re-created. See #230.
        ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                GamesHomeFragment.Companion.newInstance(), R.id.fragment);

        expandToolbar();
        selectedFragment = GAMES_FRAGMENT_ID;
    }

    public void setStandingsFragment() {
        setTitle("Standings");
        getSupportActionBar().setSubtitle(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setElevation(UnitUtils.convertDpToPixel(4, this));
        }

        Fragment standingsFragment = null;
        if (selectedFragment == STANDINGS_FRAGMENT_ID) {
            standingsFragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
            if(!(standingsFragment instanceof StandingsFragment)) {
                standingsFragment = null;
            }
        }

        if (standingsFragment == null) {
            standingsFragment = StandingsFragment.newInstance();
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    standingsFragment, R.id.fragment);
        }

        expandToolbar();
        selectedFragment = STANDINGS_FRAGMENT_ID;
    }

    public void setPlayoffsFragment() {
        setTitle("Playoff Bracket");
        getSupportActionBar().setSubtitle(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setElevation(UnitUtils.convertDpToPixel(4, this));
        }

        Fragment bracketFragment = null;
        if (selectedFragment == PLAYOFF_FRAGMENT_ID) {
            bracketFragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
            if(!(bracketFragment instanceof BracketFragment)) {
                bracketFragment = null;
            }
        }

        if (bracketFragment == null) {
            bracketFragment = BracketFragment.Companion.newInstance();
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    bracketFragment, R.id.fragment);
        }

        expandToolbar();
        selectedFragment = PLAYOFF_FRAGMENT_ID;
    }

    public void setPostsFragment(String subreddit) {
        setTitle("r/" + subreddit);
        getSupportActionBar().setSubtitle(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setElevation(UnitUtils.convertDpToPixel(4, this));
        }

        Fragment postsFragment = null;
        if (selectedFragment == POSTS_FRAGMENT_ID) {
            postsFragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
            if(!(postsFragment instanceof PostsFragment)) {
                postsFragment = null;
            }
        }

        if (postsFragment == null || !this.subreddit.equals(subreddit)) {
            postsRepository.clearCache();
            postsFragment = PostsFragment.newInstance(subreddit);
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    postsFragment, R.id.fragment);
        }

        expandToolbar();
        selectedFragment = POSTS_FRAGMENT_ID;
        this.subreddit = subreddit;
    }

    public void setHighlightsFragment() {
        setTitle("Highlights");
        getSupportActionBar().setSubtitle(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setElevation(0);
        }

        Fragment highlightsFragment = null;
        if (selectedFragment == HIGHLIGHTS_FRAGMENT_ID) {
            highlightsFragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
            if(!(highlightsFragment instanceof HighlightsMenuFragment)) {
                highlightsFragment = null;
            }
        }

        if (highlightsFragment == null) {
            highlightsFragment = HighlightsMenuFragment.Companion.newInstance();
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    highlightsFragment, R.id.fragment);
        }

        expandToolbar();
        selectedFragment = HIGHLIGHTS_FRAGMENT_ID;
    }

    private void loadRedditUsername() {
        if (navigationView == null) {
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        TextView redditUsername = headerView.findViewById(R.id.redditUsername);

        String username = localRepository.getUsername();
        if (StringUtils.Companion.isNullOrEmpty(username)) {
            redditUsername.setText(R.string.log_in);
        } else {
            redditUsername.setText(localRepository.getUsername());
        }

        redditUsername.setOnClickListener(v -> {
            // Start LoginActivity if no user is currently logged in.
            if (StringUtils.Companion.isNullOrEmpty(localRepository.getUsername())) {
                Intent loginIntent = new Intent(getApplicationContext(), LoginActivity
                        .class);
                startActivity(loginIntent);
            } else {
                Intent profileIntent = new Intent(getApplicationContext(),
                        ProfileActivity.class);
                startActivity(profileIntent);
            }
        });
    }

    private void setupDynamicShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            String teamSub = getTeamSubFromFavoritePref();
            if (teamSub != null) {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, TEAM_SUB_SHORTCUT_ID)
                        .setShortLabel(teamSub)
                        .setLongLabel(teamSub)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_r))
                        .setIntent(new Intent(this, MainActivity.class).setAction(Intent
                                .ACTION_VIEW)
                                .putExtra(SHORTCUT_KEY, SHORTCUT_TEAM_SUB))
                        .build();

                shortcutManager.setDynamicShortcuts(Arrays.asList(shortcutInfo));
            }
        }
    }

    private String getTeamSubFromFavoritePref() {
        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String favTeamVal = defaultPreferences.getString("teams_list", null);
        if (favTeamVal != null && !favTeamVal.equals(NO_FAV_TEAM_VAL)) {
            return RedditUtils.getSubredditFromAbbr(favTeamVal);
        }
        return null;
    }

    private void expandToolbar() {
        if (toolbar.getParent() instanceof AppBarLayout) {
            ((AppBarLayout) toolbar.getParent()).setExpanded(true, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        switch (selectedFragment) {
            case GAMES_FRAGMENT_ID:
                // Exit application.
                super.onBackPressed();
                break;
            default:
                // Return to games fragment.
                setGamesFragment();
                navigationView.getMenu().getItem(0).setChecked(true);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRedditUsername();
        updateNavViewFavoriteTeam();
        setGoPremiumVisibility(premiumService.isPremium());
    }

    private void setGoPremiumVisibility(boolean isPremium) {
        if (navigationView != null) {
            Menu navMenu = navigationView.getMenu();
            navMenu.findItem(R.id.go_premium).setVisible(!isPremium);
        }
    }

    private void onGoPremiumClick() {
        Bundle params = new Bundle();
        params.putString(SwishEventParam.GO_PREMIUM_ORIGIN.getKey(),
                GoPremiumOrigin.NAVIGATION_DRAWER.getOriginName());
        eventLogger.logEvent(SwishEvent.GO_PREMIUM, params);

        if (premiumService.isPremium()) {
            Toast.makeText(this, R.string.you_are_a_premium_user_already, Toast.LENGTH_SHORT)
                    .show();
            setGoPremiumVisibility(true);
        } else {
            Intent intent = new Intent(this, GoPremiumActivity.class);
            startActivity(intent);
        }
    }

    private void loadThemeToggleIcon() {
        if (navigationView == null) {
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        ImageButton toggleBtn = headerView.findViewById(R.id.themeToggle);

        SwishTheme theme = localRepository.getAppTheme();
        switch (theme) {
            case LIGHT:
                toggleBtn.setImageResource(R.drawable.ic_moon_outline);
                break;
            case DARK:
                toggleBtn.setImageResource(R.drawable.ic_moon_filled);
                break;
        }

        toggleBtn.setOnClickListener(v -> onThemeToggle());
    }

    public void onThemeToggle() {
        SwishTheme swishTheme = localRepository.getAppTheme();
        if (swishTheme == SwishTheme.LIGHT) {
            localRepository.saveAppTheme(SwishTheme.DARK);
        } else {
            localRepository.saveAppTheme(SwishTheme.LIGHT);
        }
        recreate();
    }

    private void updateNavViewFavoriteTeam() {
        if (navigationView != null) {
            String favTeam = localRepository.getFavoriteTeam();
            if (favTeam != null) {
                Menu navMenu = navigationView.getMenu();
                int resId;
                try {
                    resId = getResources().getIdentifier("nav_" + favTeam, "id", getPackageName());
                } catch (Exception e) {
                    resId = -1;
                }
                if (resId != -1) {
                    String itemTitle = navMenu.findItem(resId).getTitle().toString();
                    navMenu.removeItem(resId);
                    navMenu.add(R.id.group_team_subreddits, resId, 0, itemTitle);
                }
            }
        }
    }

    private void setupRemoteConfig() {
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        FirebaseRemoteConfig.getInstance().setConfigSettings(configSettings);
        FirebaseRemoteConfig.getInstance().setDefaults(R.xml.remote_config_defaults);

        int cacheExpiration = 3600 * 12; // 12 hours.
        if (FirebaseRemoteConfig.getInstance().getInfo().getConfigSettings()
                .isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        FirebaseRemoteConfig.getInstance().fetch(cacheExpiration)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseRemoteConfig.getInstance().activateFetched();
                    }
                });
    }
}
