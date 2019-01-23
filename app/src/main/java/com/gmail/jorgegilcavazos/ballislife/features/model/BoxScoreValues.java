package com.gmail.jorgegilcavazos.ballislife.features.model;

import com.google.gson.annotations.SerializedName;

public class BoxScoreValues {

    private BoxScoreTeam hls;
    private BoxScoreTeam vls;
    @SerializedName("p") private int periods;

    public BoxScoreValues(BoxScoreTeam hls, BoxScoreTeam vls, int periods) {
        this.hls = hls;
        this.vls = vls;
        this.periods = periods;
    }

    public BoxScoreTeam getHls() {
        return hls;
    }

    public void setHls(BoxScoreTeam hls) {
        this.hls = hls;
    }

    public BoxScoreTeam getVls() {
        return vls;
    }

    public void setVls(BoxScoreTeam vls) {
        this.vls = vls;
    }

    public int getPeriods() {
        return periods;
    }

    public void setPeriods(int periods) {
        this.periods = periods;
    }
}
