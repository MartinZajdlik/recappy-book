package cz.martinzajdlik.recappy_book.service;

import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.MealPlanRepository;
import cz.martinzajdlik.recappy_book.repository.PasswordResetTokenRepository;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import cz.martinzajdlik.recappy_book.repository.RefreshTokenRepository;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import cz.martinzajdlik.recappy_book.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * Jedno místo, kde se maže uživatel se všemi jeho vazbami.
 * Pořadí je důležité – dokud existuje řádek odkazující na uživatele
 * (refresh token, jídelníček, oblíbené recepty), databáze smazání odmítne
 * kvůli cizímu klíči.
 */
@Service
public class UserDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(UserDeletionService.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MealPlanRepository mealPlanRepository;
    private final RecipeRepository recipeRepository;
    private final ImageStorageService imageStorageService;

    public UserDeletionService(UserRepository userRepository,
                               VerificationTokenRepository verificationTokenRepository,
                               PasswordResetTokenRepository passwordResetTokenRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               MealPlanRepository mealPlanRepository,
                               RecipeRepository recipeRepository,
                               ImageStorageService imageStorageService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.recipeRepository = recipeRepository;
        this.imageStorageService = imageStorageService;
    }

    /**
     * Uživatele načítá znovu uvnitř transakce – jinak by byl odpojený (detached)
     * a práce s jeho líně načítanými kolekcemi by skončila LazyInitializationException.
     */
    @Transactional
    public void deleteUserCompletely(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        // 1) Tokeny navázané na uživatele
        verificationTokenRepository.deleteByUser_Id(userId);
        passwordResetTokenRepository.deleteAllByUser_Id(userId);
        refreshTokenRepository.deleteAllByUser_Id(userId);

        // 2) Jídelníček
        mealPlanRepository.deleteAllByUser_Id(userId);

        // 3) Oblíbené recepty, které si uživatel označil (vazební tabulka favorite_recipes)
        user.getFavoriteRecipes().clear();

        // 4) Recepty uživatele – nejdřív je odeber z oblíbených ostatním uživatelům,
        //    jinak by cizí klíč ve favorite_recipes zabránil smazání.
        List<Recipe> ownRecipes = recipeRepository.findByAuthor_Id(userId);
        for (Recipe recipe : ownRecipes) {
            for (User fan : recipe.getLikedByUsers()) {
                fan.getFavoriteRecipes().remove(recipe);
            }
            recipe.getLikedByUsers().clear();
        }

        // Vazební řádky musí jít do DB dřív než samotné DELETE receptů/uživatele
        userRepository.flush();

        for (Recipe recipe : ownRecipes) {
            deleteImageQuietly(recipe.getImageUrl());
            recipeRepository.delete(recipe);
        }
        recipeRepository.flush();

        // 5) Nakonec samotný uživatel
        userRepository.delete(user);
    }

    /**
     * Selhání Cloudinary nesmí shodit mazání účtu – obrázek pak jen zůstane viset v úložišti.
     */
    private void deleteImageQuietly(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            imageStorageService.delete(imageUrl);
        } catch (IOException | RuntimeException e) {
            logger.warn("Nepodařilo se smazat obrázek {}: {}", imageUrl, e.getMessage());
        }
    }
}
