package cz.martinzajdlik.recappy_book.dto;

import cz.martinzajdlik.recappy_book.model.MealPlanEntry;

public class MealPlanEntryResponse {

    private int dayOfWeek;
    private String breakfast;
    private String snack1;
    private String lunch;
    private String snack2;
    private String dinner;

    public MealPlanEntryResponse(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public MealPlanEntryResponse(MealPlanEntry entry) {
        this.dayOfWeek = entry.getDayOfWeek();
        this.breakfast = entry.getBreakfast();
        this.snack1 = entry.getSnack1();
        this.lunch = entry.getLunch();
        this.snack2 = entry.getSnack2();
        this.dinner = entry.getDinner();
    }

    public int getDayOfWeek() { return dayOfWeek; }
    public String getBreakfast() { return breakfast; }
    public String getSnack1() { return snack1; }
    public String getLunch() { return lunch; }
    public String getSnack2() { return snack2; }
    public String getDinner() { return dinner; }
}
