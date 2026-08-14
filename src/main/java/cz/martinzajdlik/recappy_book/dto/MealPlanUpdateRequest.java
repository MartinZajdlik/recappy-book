package cz.martinzajdlik.recappy_book.dto;

public record MealPlanUpdateRequest(
        String breakfast,
        String snack1,
        String lunch,
        String snack2,
        String dinner
) {}
