package com.placement.portal.application;

public class SelectionEligibility {
    private final boolean eligible;
    private final String reason;
    private final int requiredRounds;
    private final int completedRounds;
    private final int passedRounds;

    public SelectionEligibility(boolean eligible, String reason, int requiredRounds, int completedRounds, int passedRounds) {
        this.eligible = eligible;
        this.reason = reason;
        this.requiredRounds = requiredRounds;
        this.completedRounds = completedRounds;
        this.passedRounds = passedRounds;
    }

    public static SelectionEligibility ready(int totalRounds) {
        return new SelectionEligibility(true, "READY", totalRounds, totalRounds, totalRounds);
    }

    public static SelectionEligibility notReady(String reason, int requiredRounds, int completedRounds, int passedRounds) {
        return new SelectionEligibility(false, reason, requiredRounds, completedRounds, passedRounds);
    }

    public boolean isEligible() {
        return eligible;
    }

    public String getReason() {
        return reason;
    }

    public int getRequiredRounds() {
        return requiredRounds;
    }

    public int getCompletedRounds() {
        return completedRounds;
    }

    public int getPassedRounds() {
        return passedRounds;
    }

    @Override
    public String toString() {
        return "SelectionEligibility{" +
                "eligible=" + eligible +
                ", reason='" + reason + '\'' +
                ", requiredRounds=" + requiredRounds +
                ", completedRounds=" + completedRounds +
                ", passedRounds=" + passedRounds +
                '}';
    }
}
