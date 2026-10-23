package com.placement.portal.application;

public class ScoreCompletionDto {
    private boolean oaCompleted;
    private boolean technicalCompleted;
    private boolean hrCompleted;
    private boolean allRoundsCompleted;
    private boolean selectionEligible;
    private String readinessReason;
    private int requiredRounds;
    private int completedRounds;
    private int passedRounds;

    public ScoreCompletionDto() {}

    public ScoreCompletionDto(boolean oaCompleted, boolean technicalCompleted, boolean hrCompleted, boolean allRoundsCompleted) {
        this.oaCompleted = oaCompleted;
        this.technicalCompleted = technicalCompleted;
        this.hrCompleted = hrCompleted;
        this.allRoundsCompleted = allRoundsCompleted;
        this.selectionEligible = allRoundsCompleted;
        this.readinessReason = allRoundsCompleted ? "READY" : "Pending mandatory rounds";
        this.requiredRounds = 3;
        this.completedRounds = (oaCompleted ? 1 : 0) + (technicalCompleted ? 1 : 0) + (hrCompleted ? 1 : 0);
        this.passedRounds = this.completedRounds;
    }

    public ScoreCompletionDto(boolean oaCompleted, boolean technicalCompleted, boolean hrCompleted,
                              boolean allRoundsCompleted, boolean selectionEligible, String readinessReason,
                              int requiredRounds, int completedRounds, int passedRounds) {
        this.oaCompleted = oaCompleted;
        this.technicalCompleted = technicalCompleted;
        this.hrCompleted = hrCompleted;
        this.allRoundsCompleted = allRoundsCompleted;
        this.selectionEligible = selectionEligible;
        this.readinessReason = readinessReason;
        this.requiredRounds = requiredRounds;
        this.completedRounds = completedRounds;
        this.passedRounds = passedRounds;
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

    public boolean isSelectionEligible() {
        return selectionEligible;
    }

    public void setSelectionEligible(boolean selectionEligible) {
        this.selectionEligible = selectionEligible;
    }

    public String getReadinessReason() {
        return readinessReason;
    }

    public void setReadinessReason(String readinessReason) {
        this.readinessReason = readinessReason;
    }

    public int getRequiredRounds() {
        return requiredRounds;
    }

    public void setRequiredRounds(int requiredRounds) {
        this.requiredRounds = requiredRounds;
    }

    public int getCompletedRounds() {
        return completedRounds;
    }

    public void setCompletedRounds(int completedRounds) {
        this.completedRounds = completedRounds;
    }

    public int getPassedRounds() {
        return passedRounds;
    }

    public void setPassedRounds(int passedRounds) {
        this.passedRounds = passedRounds;
    }
}
