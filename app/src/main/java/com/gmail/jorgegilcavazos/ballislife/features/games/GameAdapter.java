package com.gmail.jorgegilcavazos.ballislife.features.games;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.features.model.Broadcaster;
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2;
import com.gmail.jorgegilcavazos.ballislife.features.model.MediaSource;
import com.gmail.jorgegilcavazos.ballislife.features.model.NbaGame;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil;
import com.gmail.jorgegilcavazos.ballislife.util.StringUtils;
import com.gmail.jorgegilcavazos.ballislife.util.TeamUtils;
import com.gmail.jorgegilcavazos.ballislife.util.UnitUtils;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * RecyclerView Adapter used by the {@link GamesFragment} to display a list of games.
 */
public class GameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<GameV2> nbaGameList;
    private final LocalRepository localRepository;
    private PublishSubject<GameV2> gameClicks = PublishSubject.create();

    public GameAdapter(List<GameV2> nbaGames, LocalRepository localRepository) {
        nbaGameList = nbaGames;
        this.localRepository = localRepository;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (Constants.NBA_MATERIAL_ENABLED) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_game_logos,
                    parent, false);
            return new GameViewHolder(view, localRepository);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_game_bars,
                    parent, false);
            return new GameViewHolderWithBars(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (Constants.NBA_MATERIAL_ENABLED) {
            ((GameViewHolder) holder).bindData(nbaGameList.get(position), gameClicks,
                                               nbaGameList.size() - 1 == position);
        } else {
            ((GameViewHolderWithBars) holder).bindData(nbaGameList.get(position), gameClicks);
        }
    }

    @Override
    public int getItemCount() {
        return null != nbaGameList ? nbaGameList.size() : 0;
    }

    public void swap(List<GameV2> data) {
        nbaGameList.clear();
        nbaGameList.addAll(data);
        notifyDataSetChanged();
    }

    public Observable<GameV2> getGameClicks() {
        return gameClicks;
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.gameCard) CardView gameCard;
        @BindView(R.id.layout_content) RelativeLayout container;
        @BindView(R.id.homelabel) TextView tvHomeTeam;
        @BindView(R.id.awaylabel) TextView tvAwayTeam;
        @BindView(R.id.homescore) TextView tvHomeScore;
        @BindView(R.id.awayscore) TextView tvAwayScore;
        @BindView(R.id.clock) TextView tvClock;
        @BindView(R.id.period) TextView tvPeriod;
        @BindView(R.id.text_time) TextView tvTime;
        @BindView(R.id.text_final) TextView tvFinal;
        @BindView(R.id.homeicon) ImageView ivHomeLogo;
        @BindView(R.id.awayicon) ImageView ivAwayLogo;
        @BindView(R.id.broadcaster) TextView tvBroadcaster;
        @BindView(R.id.seriesSummary) TextView seriesSummary;
        @BindView(R.id.favoriteMarker) View favoriteMarker;

        private final LocalRepository localRepository;

        public GameViewHolder(View view, LocalRepository localRepository) {
            super(view);
            this.localRepository = localRepository;
            ButterKnife.bind(this, view);
        }

        public void bindData(
                GameV2 nbaGame,
                PublishSubject<GameV2> gameClicks,
                boolean isLastGame) {
            int resKeyHome = TeamUtils.Companion
                    .getTeamLogo(nbaGame.getHomeTeamKey().toUpperCase());
            int resKeyAway = TeamUtils.Companion
                    .getTeamLogo(nbaGame.getAwayTeamKey().toUpperCase());

            // Set favorite team marker if game has fav team.
            favoriteMarker.setVisibility(View.GONE);
            String favoriteTeam = localRepository.getFavoriteTeam();
            if (favoriteTeam != null) {
                String favoriteTeamId = TeamUtils.Companion.getTeamId(favoriteTeam);
                if (nbaGame.getAwayTeamId().equals(favoriteTeamId)
                        || nbaGame.getHomeTeamId().equals(favoriteTeamId)) {
                    favoriteMarker.setVisibility(View.VISIBLE);
                }
            }

            ivHomeLogo.setImageResource(resKeyHome);
            ivAwayLogo.setImageResource(resKeyAway);
            tvHomeTeam.setText(nbaGame.getHomeTeamAbbr());
            tvAwayTeam.setText(nbaGame.getAwayTeamAbbr());
            tvClock.setText(nbaGame.getGameClock());
            tvPeriod.setText(Utilities.getPeriodString(nbaGame.getPeriodValue(), nbaGame
                    .getPeriodName()));

            if (localRepository.noSpoilersModeEnabled()) {
                tvHomeScore.setText("-");
                tvAwayScore.setText("-");
            } else {
                tvHomeScore.setText(nbaGame.getHomeTeamScore());
                tvAwayScore.setText(nbaGame.getAwayTeamScore());
            }

            tvHomeScore.setVisibility(View.GONE);
            tvAwayScore.setVisibility(View.GONE);
            tvClock.setVisibility(View.GONE);
            tvPeriod.setVisibility(View.GONE);
            tvFinal.setVisibility(View.GONE);
            tvTime.setVisibility(View.GONE);
            tvBroadcaster.setVisibility(View.GONE);

            if (nbaGame.getSeriesSummary() == null) {
                seriesSummary.setVisibility(View.GONE);
            } else {
                seriesSummary.setVisibility(View.VISIBLE);
                seriesSummary.setText(nbaGame.getSeriesSummary());
            }

            String nationalBroadcaster = findNationalBroadcasters(nbaGame.getBroadcasters());

            switch (nbaGame.getGameStatus()) {
                case NbaGame.PRE_GAME:
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setText(DateFormatUtil.localizeGameTime(nbaGame.getPeriodStatus()));
                    if (!nationalBroadcaster.equals("")) {
                        tvBroadcaster.setText(nationalBroadcaster);
                        tvBroadcaster.setVisibility(View.VISIBLE);
                    }
                    break;
                case NbaGame.IN_GAME:
                    tvHomeScore.setVisibility(View.VISIBLE);
                    tvAwayScore.setVisibility(View.VISIBLE);
                    if (nbaGame.getPeriodStatus().equals("Halftime")) {
                        tvFinal.setVisibility(View.VISIBLE);
                        tvFinal.setText(R.string.halftime);
                    } else {
                        tvClock.setVisibility(View.VISIBLE);
                        tvPeriod.setVisibility(View.VISIBLE);
                    }
                    break;
                case NbaGame.POST_GAME:
                    tvHomeScore.setVisibility(View.VISIBLE);
                    tvAwayScore.setVisibility(View.VISIBLE);
                    tvFinal.setVisibility(View.VISIBLE);
                    tvFinal.setText(R.string.end_of_game);
                    break;
            }

            RxView.clicks(container).map(v -> nbaGame).subscribe(gameClicks);

            int margin = (int) UnitUtils.convertDpToPixel(8, itemView.getContext());
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) gameCard
                    .getLayoutParams();
            if (isLastGame) {
                layoutParams.setMargins(margin, margin, margin, margin);
            } else {
                layoutParams.setMargins(margin, margin, margin, 0);
            }
            gameCard.requestLayout();
        }
    }

    public static class GameViewHolderWithBars extends RecyclerView.ViewHolder {

        @BindView(R.id.layout_content) RelativeLayout container;
        @BindView(R.id.homelabel) TextView tvHomeTeam;
        @BindView(R.id.awaylabel) TextView tvAwayTeam;
        @BindView(R.id.homescore) TextView tvHomeScore;
        @BindView(R.id.awayscore) TextView tvAwayScore;
        @BindView(R.id.clock) TextView tvClock;
        @BindView(R.id.period) TextView tvPeriod;
        @BindView(R.id.text_time) TextView tvTime;
        @BindView(R.id.text_final) TextView tvFinal;
        @BindView(R.id.away_bar) View barAway;
        @BindView(R.id.home_bar) View barHome;

        public GameViewHolderWithBars(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void bindData(GameV2 nbaGame, PublishSubject<GameV2> gameClicks) {
            int resKeyHome = itemView.getContext().getResources().getIdentifier(nbaGame
                    .getHomeTeamAbbr().toLowerCase(), "color", itemView.getContext()
                    .getPackageName());
            int resKeyAway = itemView.getContext().getResources().getIdentifier(nbaGame
                    .getAwayTeamAbbr().toLowerCase(), "color", itemView.getContext()
                    .getPackageName());

            if (resKeyAway != 0) {
                ViewCompat.setBackgroundTintList(barAway, itemView.getContext().getResources()
                        .getColorStateList(resKeyAway));
            }
            if (resKeyHome != 0) {
                ViewCompat.setBackgroundTintList(barHome, itemView.getContext().getResources()
                        .getColorStateList(resKeyHome));
            }

            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, itemView
                    .getContext().getResources().getDisplayMetrics());
            float awayPct;
            float homePct;
            try {
                int awayScore = Integer.valueOf(nbaGame.getAwayTeamScore());
                int homeScore = Integer.valueOf(nbaGame.getHomeTeamScore());
                awayPct = (float) (awayScore) / (float) (awayScore + homeScore);
                homePct = (float) (homeScore) / (float) (awayScore + homeScore);

                if (awayScore == 0 && homeScore == 0) {
                    awayPct = 0.5f;
                    homePct = 0.5f;
                }
            } catch (NumberFormatException e) {
                awayPct = 0.5f;
                homePct = 0.5f;
            }
            barHome.setLayoutParams(new TableLayout.LayoutParams(0, height, awayPct));
            barAway.setLayoutParams(new TableLayout.LayoutParams(0, height, homePct));

            tvHomeTeam.setText(nbaGame.getHomeTeamAbbr());
            tvAwayTeam.setText(nbaGame.getAwayTeamAbbr());
            tvHomeScore.setText(nbaGame.getHomeTeamScore());
            tvAwayScore.setText(nbaGame.getAwayTeamScore());
            tvClock.setText(nbaGame.getGameClock());
            tvPeriod.setText(Utilities.getPeriodString(nbaGame.getPeriodValue(), nbaGame
                    .getPeriodName()));

            tvHomeScore.setVisibility(View.GONE);
            tvAwayScore.setVisibility(View.GONE);
            tvClock.setVisibility(View.GONE);
            tvPeriod.setVisibility(View.GONE);
            tvFinal.setVisibility(View.GONE);
            tvTime.setVisibility(View.GONE);

            switch (nbaGame.getGameStatus()) {
                case NbaGame.PRE_GAME:
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setText(DateFormatUtil.localizeGameTime(nbaGame.getPeriodStatus()));
                    break;
                case NbaGame.IN_GAME:
                    tvHomeScore.setVisibility(View.VISIBLE);
                    tvAwayScore.setVisibility(View.VISIBLE);
                    tvClock.setVisibility(View.VISIBLE);
                    tvPeriod.setVisibility(View.VISIBLE);
                    break;
                case NbaGame.POST_GAME:
                    tvHomeScore.setVisibility(View.VISIBLE);
                    tvAwayScore.setVisibility(View.VISIBLE);
                    tvFinal.setVisibility(View.VISIBLE);
                    tvFinal.setText("FINAL");
                    break;
            }

            RxView.clicks(container).map(v -> nbaGame).subscribe(gameClicks);
        }
    }

    private static String findNationalBroadcasters(Map<String, MediaSource> mapMediaSources) {
        if (mapMediaSources == null) {
            return "";
        }
        String brodcasters = "";
        for (Map.Entry<String, MediaSource> entry : mapMediaSources.entrySet()) {
            if (entry.getKey().equals("tv")) {
                for (Broadcaster broadcaster : entry.getValue().getBroadcaster()) {
                    if (broadcaster.getScope().equals("natl")) {
                        brodcasters += broadcaster.getDisplayName() + "/";
                    }
                }
            }
        }
        if (!StringUtils.Companion.isNullOrEmpty(brodcasters)) {
            // Remove last "/"
            brodcasters = brodcasters.substring(0, brodcasters.length() - 1);
        }
        return brodcasters;
    }
}
