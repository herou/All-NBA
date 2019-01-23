package com.gmail.jorgegilcavazos.ballislife.features.gamethread;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.features.boxscore.BoxScoreFragment;
import com.gmail.jorgegilcavazos.ballislife.features.model.GameThreadType;

import java.util.HashMap;
import java.util.Map;

/**
 * Pager for the TabLayout in CommentsActivity.
 */
public class PagerAdapter extends FragmentStatePagerAdapter {

    private final Context context;
    private final Bundle bundle;
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    static final int GAME_THREAD_TAB = 0;
    static final int STATS_TAB = 1;
    static final int POST_GAME_TAB = 2;

    public PagerAdapter(Context context, FragmentManager fragmentManager, Bundle bundle) {
        super(fragmentManager);
        this.context = context;
        this.bundle = bundle;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case GAME_THREAD_TAB:
                Bundle liveBundle = (Bundle) bundle.clone();
                liveBundle.putSerializable(GameThreadFragment.THREAD_TYPE_KEY, GameThreadType.LIVE);
                if (fragmentMap.get(0) != null) {
                    return fragmentMap.get(position);
                } else {
                    GameThreadFragment tab1 = GameThreadFragment.newInstance();
                    tab1.setArguments(liveBundle);
                    fragmentMap.put(0, tab1);
                    return tab1;
                }
            case STATS_TAB:
                BoxScoreFragment tab2 = new BoxScoreFragment();
                tab2.setArguments(bundle);
                fragmentMap.put(1, tab2);
                return tab2;
            case POST_GAME_TAB:
                Bundle postBundle = (Bundle) bundle.clone();
                postBundle.putSerializable(GameThreadFragment.THREAD_TYPE_KEY, GameThreadType.POST);
                if (fragmentMap.get(2) != null) {
                    return fragmentMap.get(2);
                } else {
                    GameThreadFragment tab3 = GameThreadFragment.newInstance();
                    tab3.setArguments(postBundle);
                    fragmentMap.put(2, tab3);
                    return tab3;
                }
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case GAME_THREAD_TAB:
                return context.getString(R.string.game_thread);
            case STATS_TAB:
                return context.getString(R.string.stats_tab_text);
            case POST_GAME_TAB:
                return context.getString(R.string.post_game_thread);
        }

        throw new IllegalArgumentException("Position exceeded support tabs: " + position);
    }
}