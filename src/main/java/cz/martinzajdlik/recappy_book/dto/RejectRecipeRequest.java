package cz.martinzajdlik.recappy_book.dto;

/**
 * Tělo požadavku na zamítnutí receptu. Důvod je nepovinný – starší verze
 * iOS appky posílají zamítnutí úplně bez těla.
 */
public record RejectRecipeRequest(String reason) {}
