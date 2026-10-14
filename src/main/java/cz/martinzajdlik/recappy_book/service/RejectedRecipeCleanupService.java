package cz.martinzajdlik.recappy_book.service;

import cz.martinzajdlik.recappy_book.model.RecipeStatus;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Pravidelný úklid: maže POUZE zamítnuté recepty (status REJECTED), se kterými
 * autor po zamítnutí RETENTION_DAYS dní nic neudělal. Schválené ani čekající
 * recepty se nikdy nemažou – dotaz i následná kontrola filtrují jen REJECTED.
 *
 * Úprava receptu autorem ho vrátí do PENDING a vynuluje rejectedAt, tím se
 * odpočet zastaví. Schválení taky.
 *
 * Vypnutí: proměnná prostředí REJECTED_RECIPE_CLEANUP_ENABLED=false.
 */
@Service
public class RejectedRecipeCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(RejectedRecipeCleanupService.class);

    /** Po kolika dnech od zamítnutí se recept smaže. Používá i RecipeResponse pro zobrazení data autorovi. */
    public static final int RETENTION_DAYS = 30;

    /** Pojistka: nejvýš tolik receptů za jeden běh. Zbytek se smaže v dalších bězích. */
    private static final int MAX_DELETIONS_PER_RUN = 50;

    private final RecipeRepository recipeRepository;
    private final RejectedRecipeDeletionService deletionService;
    private final ImageStorageService imageStorageService;
    private final boolean enabled;

    public RejectedRecipeCleanupService(RecipeRepository recipeRepository,
                                        RejectedRecipeDeletionService deletionService,
                                        ImageStorageService imageStorageService,
                                        @Value("${app.cleanup.rejected-recipes.enabled:true}") boolean enabled) {
        this.recipeRepository = recipeRepository;
        this.deletionService = deletionService;
        this.imageStorageService = imageStorageService;
        this.enabled = enabled;
    }

    // Poprvé 10 minut po startu, pak každých 6 hodin. Interval místo pevného času,
    // protože Render free tier může v daný čas zrovna spát.
    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT6H")
    public void cleanupExpiredRejectedRecipes() {
        if (!enabled) {
            logger.info("Úklid zamítnutých receptů je vypnutý (REJECTED_RECIPE_CLEANUP_ENABLED=false).");
            return;
        }

        try {
            Instant now = Instant.now();

            int filled = recipeRepository.fillMissingRejectedAt(RecipeStatus.REJECTED, now);
            if (filled > 0) {
                logger.info("Úklid: {} zamítnutým receptům bez data bylo nastaveno datum zamítnutí na teď.", filled);
            }

            Instant cutoff = now.minus(RETENTION_DAYS, ChronoUnit.DAYS);
            List<Long> candidateIds = recipeRepository.findIdsByStatusAndRejectedAtBefore(RecipeStatus.REJECTED, cutoff);

            if (candidateIds.isEmpty()) {
                return;
            }

            List<Long> batch = candidateIds.stream().limit(MAX_DELETIONS_PER_RUN).toList();
            logger.info("Úklid: {} zamítnutých receptů starších {} dní, tento běh zpracuje {}.",
                    candidateIds.size(), RETENTION_DAYS, batch.size());

            int deleted = 0;
            for (Long id : batch) {
                try {
                    var result = deletionService.deleteIfExpired(id, cutoff);
                    if (result.isPresent()) {
                        deleted++;
                        logger.info("Úklid: smazán zamítnutý recept id={} „{}“.", result.get().id(), result.get().title());
                        deleteImageQuietly(result.get().imageUrl());
                    }
                } catch (RuntimeException e) {
                    logger.warn("Úklid: recept id={} se nepodařilo smazat: {}", id, e.getMessage());
                }
            }

            logger.info("Úklid zamítnutých receptů hotov, smazáno {}.", deleted);
        } catch (RuntimeException e) {
            // Chyba úklidu nesmí nijak ovlivnit běh aplikace.
            logger.error("Úklid zamítnutých receptů selhal: {}", e.getMessage(), e);
        }
    }

    private void deleteImageQuietly(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            imageStorageService.delete(imageUrl);
        } catch (IOException | RuntimeException e) {
            logger.warn("Úklid: nepodařilo se smazat obrázek {}: {}", imageUrl, e.getMessage());
        }
    }
}
