package com.gmail.jorgegilcavazos.ballislife.features.boxscore;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.gamestats.QuarterByQuarterScoreComponent;
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.CommentsActivity;
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreValues;
import com.gmail.jorgegilcavazos.ballislife.features.model.StatLine;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishTheme;
import com.gmail.jorgegilcavazos.ballislife.features.model.Team;
import com.gmail.jorgegilcavazos.ballislife.util.UnitUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Optional;
import com.jakewharton.rxrelay2.PublishRelay;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class BoxScoreFragment extends Fragment implements BoxScoreView {

    @Inject BoxScorePresenter presenter;
    @Inject LocalRepository localRepository;
    @Inject PremiumService premiumService;

    @BindView(R.id.button_home) Button btnHome;
    @BindView(R.id.button_away) Button btnAway;
    @BindView(R.id.text_load_message) TextView tvLoadMessage;
    @BindView(R.id.playersTable) TableLayout playersTable;
    @BindView(R.id.statsTable) TableLayout statsTable;
    @BindView(R.id.scrollView) ScrollView scrollView;
    @BindView(R.id.adView) AdView adView;
    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.qtrByQtrScoreContainer) ViewGroup qtrByQtrScoreContainer;

    private Unbinder unbinder;

    private String homeTeam;
    private String awayTeam;
    private String gameId;
    private BoxScoreSelectedTeam teamSelected;

    private PublishRelay<QuarterByQuarterScoreComponent.Event> qtrByQtrScoreEvents =
            PublishRelay.create();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        BallIsLifeApplication.getAppComponent().inject(this);

        if (getArguments() != null) {
            homeTeam = getArguments().getString(CommentsActivity.HOME_TEAM_KEY);
            awayTeam = getArguments().getString(CommentsActivity.AWAY_TEAM_KEY);
            gameId = getArguments().getString(CommentsActivity.GAME_ID_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_box_score, container, false);
        unbinder = ButterKnife.bind(this, view);

        teamSelected = BoxScoreSelectedTeam.VISITOR;
        setHomeAwayTextColor();
        setHomeAwayBackground();

        btnAway.setText(awayTeam);
        btnHome.setText(homeTeam);

        presenter.attachView(this);
        presenter.loadBoxScore(gameId, teamSelected, true /* forceNetwork */);

        swipeRefreshLayout.setOnRefreshListener(() ->
                presenter.loadBoxScore(gameId, teamSelected, true)
        );

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (premiumService.isPremium()) {
            adView.setVisibility(View.GONE);
        } else {
            adView.setVisibility(View.VISIBLE);
            adView.loadAd(new AdRequest.Builder().build());
        }

        new QuarterByQuarterScoreComponent(
                qtrByQtrScoreContainer,
                qtrByQtrScoreEvents,
                Team.Companion.fromKey(homeTeam),
                Team.Companion.fromKey(awayTeam)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if the user is premium or if the game is unlocked and hide ads if either is
        // true, but don't load the ad again if false. This is necessary for when the user
        // watches a rewarded video or purchases premium in a different activity and returns to
        // this screen.
        if (premiumService.isPremium() || localRepository.isGameStreamUnlocked(gameId)) {
            adView.setVisibility(View.GONE);
        } else {
            adView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        presenter.detachView();
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_box_score, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                presenter.loadBoxScore(gameId, teamSelected, true /* forceNetwork */);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.button_away)
    public void onButtonAwayClick() {
        teamSelected = BoxScoreSelectedTeam.VISITOR;
        setHomeAwayTextColor();
        setHomeAwayBackground();

        presenter.loadBoxScore(gameId, teamSelected, false /* forceNetwork */);
    }

    @OnClick(R.id.button_home)
    public void onButtonHomeClick() {
        teamSelected = BoxScoreSelectedTeam.HOME;
        setHomeAwayTextColor();
        setHomeAwayBackground();

        presenter.loadBoxScore(gameId, teamSelected, false /* forceNetwork */);
    }

    @Override
    public void showVisitorBoxScore(@NonNull BoxScoreValues values) {
        List<String> players = new ArrayList<>();

        for (StatLine statLine : values.getVls().getPstsg()) {
            // Some players don't have a first name, like Nene.
            if (statLine.getFn() != null && statLine.getFn().length() >= 1) {
                players.add(statLine.getFn().substring(0, 1) + ". " + statLine.getLn());
            } else {
                players.add(statLine.getLn());
            }
        }
        players.add("TOTAL");

        int i = 1;
        addRowToPlayersTable2("PLAYER");
        for (String player : players) {
            addRowToPlayersTable2(player);
            if (i == 5 || i == players.size()-1) {
                addSeparatorRowToPlayers();
            }
            i++;
        }

        StatLine total = new StatLine(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,"0","0");
        i = 1;
        addRowToStatsTable2(Optional.absent());
        for (StatLine statLine : values.getVls().getPstsg()) {
            addRowToStatsTable2(Optional.of(statLine));
            addToTeamTotalStats(statLine, total);
            if (i == 5 || i == players.size()-1) {
                addSeparatorRowToStats(21); // # Stats + 3 for fg%, 3pt%, ft%
            }
            i++;
        }
        displayTeamTotalStats(total);
        scrollView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showHomeBoxScore(@NonNull BoxScoreValues values) {
        List<String> players = new ArrayList<>();

        for (StatLine statLine : values.getHls().getPstsg()) {
            // Some players don't have a first name, like Nene.
            if (statLine.getFn() != null && statLine.getFn().length() >= 1) {
                players.add(statLine.getFn().substring(0, 1) + ". " + statLine.getLn());
            } else {
                players.add(statLine.getLn());
            }
        }
        players.add("TOTAL");

        addRowToPlayersTable2("PLAYER");
        int i = 1;
        for (String player : players) {
            addRowToPlayersTable2(player);
            if (i == 5 || i == players.size()-1) {
                addSeparatorRowToPlayers();
            }
            i++;
        }

        StatLine total = new StatLine(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,"0","0");
        i = 1;
        addRowToStatsTable2(Optional.absent());
        for (StatLine statLine : values.getHls().getPstsg()) {
            addRowToStatsTable2(Optional.of(statLine));
            addToTeamTotalStats(statLine, total);
            if (i == 5 || i == players.size()-1) {
                addSeparatorRowToStats(21); // # Stats + 3 for fg%, 3pt%, ft%
            }
            i++;
        }
        displayTeamTotalStats(total);
        scrollView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showQuarterByQuarterTable(@NotNull QuarterByQuarterStats stats) {
        qtrByQtrScoreEvents.accept(new QuarterByQuarterScoreComponent.Event.StatsUpdated(stats));
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void hideBoxScore() {
        scrollView.setVisibility(View.GONE);
        playersTable.removeAllViews();
        statsTable.removeAllViews();
    }

    @Override
    public void showBoxScoreNotAvailableMessage(boolean active) {
        if (active) {
            tvLoadMessage.setText(R.string.box_score_not_available);
            tvLoadMessage.setVisibility(View.VISIBLE);
        } else {
            tvLoadMessage.setVisibility(View.GONE);
        }
    }

    public void addRowToPlayersTable2(String content) {
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        int width = (int) UnitUtils.convertDpToPixel(100, getActivity());
        row.setMinimumWidth(width);

        if (content.equals("PLAYER")) {
            TextView tv = addHeaderItem(row, content);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            row.addView(tv);
        } else {
            TextView tv = addNormalItem(row, content);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            row.addView(tv);
        }

        playersTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }


    public void displayTeamTotalStats(StatLine statLine){
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        row.addView(addNormalItem(row, String.valueOf(statLine.getMin())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getPts())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getReb())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getAst())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getStl())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getBlk())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getBlka())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getOreb())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getDreb())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getFgm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getFga())));
        row.addView(addNormalItem(row, getShootingPct(statLine.getFga(), statLine.getFgm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getTpm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getTpa())));
        row.addView(addNormalItem(row, getShootingPct(statLine.getTpa(), statLine.getTpm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getFtm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getFta())));
        row.addView(addNormalItem(row, getShootingPct(statLine.getFta(), statLine.getFtm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getPf())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getTov())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getPm())));

        statsTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    public void addToTeamTotalStats(StatLine curr, StatLine total){
        total.setMin(curr.getMin()+total.getMin());
        total.setPts(curr.getPts()+total.getPts());
        total.setReb(curr.getReb()+total.getReb());
        total.setAst(curr.getAst()+total.getAst());
        total.setStl(curr.getStl()+total.getStl());
        total.setBlk(curr.getBlk()+total.getBlk());
        total.setBlka(curr.getBlka()+total.getBlka());
        total.setOreb(curr.getOreb()+total.getOreb());
        total.setDreb(curr.getDreb()+total.getDreb());
        total.setFgm(curr.getFgm()+total.getFgm());
        total.setFga(curr.getFga()+total.getFga());
        total.setTpm(curr.getTpm()+total.getTpm());
        total.setTpa(curr.getTpa()+total.getTpa());
        total.setFtm(curr.getFtm()+total.getFtm());
        total.setFta(curr.getFta()+total.getFta());
        total.setPf(curr.getPf()+total.getPf());
        total.setTov(curr.getTov()+total.getTov());
        total.setPm(curr.getPm()+total.getPm());
    }
    
    public void addRowToStatsTable2(Optional<StatLine> statLineOptional) {
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        if (statLineOptional.isPresent()) {
            StatLine statLine = statLineOptional.get();

            row.addView(addNormalItem(row, String.valueOf(statLine.getMin())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getPts())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getReb())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getAst())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getStl())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getBlk())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getBlka())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getOreb())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getDreb())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getFgm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getFga())));
            row.addView(addNormalItem(row, getShootingPct(statLine.getFga(), statLine.getFgm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getTpm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getTpa())));
            row.addView(addNormalItem(row, getShootingPct(statLine.getTpa(), statLine.getTpm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getFtm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getFta())));
            row.addView(addNormalItem(row, getShootingPct(statLine.getFta(), statLine.getFtm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getPf())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getTov())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getPm())));
        } else {
            row.addView(addHeaderItem(row, "MIN"));
            row.addView(addHeaderItem(row, "PTS"));
            row.addView(addHeaderItem(row, "REB"));
            row.addView(addHeaderItem(row, "AST"));
            row.addView(addHeaderItem(row, "STL"));
            row.addView(addHeaderItem(row, "BLK"));
            row.addView(addHeaderItem(row, "BA"));
            row.addView(addHeaderItem(row, "OREB"));
            row.addView(addHeaderItem(row, "DREB"));
            row.addView(addHeaderItem(row, "FGM"));
            row.addView(addHeaderItem(row, "FGA"));
            row.addView(addHeaderItem(row, "FG%"));
            row.addView(addHeaderItem(row, "3PM"));
            row.addView(addHeaderItem(row, "3PA"));
            row.addView(addHeaderItem(row, "3P%"));
            row.addView(addHeaderItem(row, "FTM"));
            row.addView(addHeaderItem(row, "FTA"));
            row.addView(addHeaderItem(row, "FT%"));
            row.addView(addHeaderItem(row, "PF"));
            row.addView(addHeaderItem(row, "TO"));
            row.addView(addHeaderItem(row, "+/-"));
        }

        statsTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    private void addSeparatorRowToPlayers() {
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        View view = LayoutInflater.from(getActivity())
                .inflate(R.layout.box_score_separator, row, false);
        row.addView(view);
        playersTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    private void addSeparatorRowToStats(int columns) {
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        for (int i = 0; i < columns; i++) {
            View view = LayoutInflater.from(getActivity())
                    .inflate(R.layout.box_score_separator, row, false);
            row.addView(view);
        }
        statsTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    private TextView addHeaderItem(TableRow row, String text) {
        TextView view = (TextView) LayoutInflater.from(getActivity())
                .inflate(R.layout.boxscore_item, row, false);
        view.setText(text);
        view.setTypeface(null, Typeface.BOLD);
        view.setMinWidth((int) UnitUtils.convertDpToPixel(30, getActivity()));
        return view;
    }

    private TextView addNormalItem(TableRow row, String text) {
        TextView view = (TextView) LayoutInflater.from(getActivity())
                .inflate(R.layout.boxscore_item, row, false);
        view.setText(text);
        view.setTypeface(null, Typeface.NORMAL);
        view.setMinWidth((int) UnitUtils.convertDpToPixel(30, getActivity()));
        return view;
    }

    private String getShootingPct(double attempts, double makes) {
        if (attempts == 0) {
            return "-";
        }

        int pct = (int) ((makes / attempts) * 100);
        return pct + "%";
    }

    private void setHomeAwayBackground() {
        if (teamSelected == BoxScoreSelectedTeam.VISITOR) {
            btnAway.setBackgroundResource(R.drawable.box_score_square_selected);
            btnHome.setBackgroundResource(R.drawable.box_score_square_unselected);
        } else {
            btnHome.setBackgroundResource(R.drawable.box_score_square_selected);
            btnAway.setBackgroundResource(R.drawable.box_score_square_unselected);
        }
    }

    private void setHomeAwayTextColor() {
        if (teamSelected == BoxScoreSelectedTeam.VISITOR) {
            btnAway.setTextColor(
                    getSelectedTextColor(getActivity(), localRepository.getAppTheme())
            );
            btnHome.setTextColor(
                    getUnselectedTextColor(getActivity(), localRepository.getAppTheme())
            );
        } else {
            btnHome.setTextColor(
                    getSelectedTextColor(getActivity(), localRepository.getAppTheme())
            );
            btnAway.setTextColor(
                    getUnselectedTextColor(getActivity(), localRepository.getAppTheme())
            );
        }
    }

    private int getSelectedTextColor(Context context, SwishTheme theme) {
        final int[] attrs = { R.attr.boxScoreSquareSelectedTextColor };
        TypedArray typedArray;
        if (theme == SwishTheme.DARK) {
            typedArray = context.obtainStyledAttributes(R.style.AppTheme_Dark, attrs);
        } else {
            typedArray = context.obtainStyledAttributes(R.style.AppTheme, attrs);
        }
        int textColor = typedArray.getColor(0, Color.BLACK);
        typedArray.recycle();
        return textColor;
    }

    private int getUnselectedTextColor(Context context, SwishTheme theme) {
        final int[] attrs = { R.attr.boxScoreSquareUnselectedTextColor };
        TypedArray typedArray;
        if (theme == SwishTheme.DARK) {
            typedArray = context.obtainStyledAttributes(R.style.AppTheme_Dark, attrs);
        } else {
            typedArray = context.obtainStyledAttributes(R.style.AppTheme, attrs);
        }
        int textColor = typedArray.getColor(0, Color.BLACK);
        typedArray.recycle();
        return textColor;
    }

    @Override
    public void showUnknownErrorToast(int code) {
        Toast.makeText(getActivity(), getString(R.string.something_went_wrong, code),
                Toast.LENGTH_SHORT).show();
    }
}
