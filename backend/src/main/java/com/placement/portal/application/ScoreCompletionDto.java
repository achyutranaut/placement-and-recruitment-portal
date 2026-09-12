package com.placement.portal.application;

public class ScoreCompletionDto {
    private boolean oaCompleted;
    private boolean technicalCompleted;
    private boolean hrCompleted;
    private boolean allRoundsCompleted;

    public ScoreCompletionDto() {}

    public ScoreCompletionDto(boolean oaCompleted, boolean technicalCompleted, boolean hrCompleted, boolean allRoundsCompleted) {
        this.oaCompleted = oaCompleted;
        this.technicalCompleted = technicalCompleted;
        this.hrCompleted = hrCompleted;
        this.allRoundsCompleted = allRoundsCompleted;
    }

    public boolean isOaCompleted() {
        return oaCompleted;
    }

    public void setOaCompleted(boolean oaCompleted) {
        this.oaCompleted = oaCompleted;
    }

    public boolean isTechnicalCompleted() {
        return technicalCompleted;
    }

    public void setTechnicalCompleted(boolean technicalCompleted) {
        this.technicalCompleted = technicalCompleted;
    }

    public boolean isHrCompleted() {
        return hrCompleted;
    }

    public void setHrCompleted(boolean hrCompleted) {
        this.hrCompleted = hrCompleted;
    }

    public boolean isAllRoundsCompleted() {
        return allRoundsCompleted;
    }

    public void setAllRoundsCompleted(boolean allRoundsCompleted) {
        this.allRoundsCompleted = allRoundsCompleted;
    }
}
