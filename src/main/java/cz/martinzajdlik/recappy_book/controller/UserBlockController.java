package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.dto.BlockedUserResponse;
import cz.martinzajdlik.recappy_book.model.BlockedUser;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.BlockedUserRepository;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Blokování jiných uživatelů – čistě soukromá věc mezi blokujícím a appkou,
 * bez zásahu admina. Zablokované recepty se blokujícímu přestanou zobrazovat
 * (viz RecipeController.blockedAuthorIdsFor).
 */
@RestController
@RequestMapping("/users")
public class UserBlockController {

    private final BlockedUserRepository blockedUserRepository;
    private final UserRepository userRepository;

    public UserBlockController(BlockedUserRepository blockedUserRepository, UserRepository userRepository) {
        this.blockedUserRepository = blockedUserRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<String> blockUser(@PathVariable Long id, Authentication authentication) {
        User blocker = userRepository.getByUsername(authentication.getName());

        if (blocker.getId().equals(id)) {
            return ResponseEntity.badRequest().body("Sám sebe zablokovat nemůžeš.");
        }

        User blocked = userRepository.findById(id).orElse(null);
        if (blocked == null) {
            return ResponseEntity.notFound().build();
        }

        if (!blockedUserRepository.existsByBlocker_IdAndBlocked_Id(blocker.getId(), id)) {
            BlockedUser entry = new BlockedUser();
            entry.setBlocker(blocker);
            entry.setBlocked(blocked);
            blockedUserRepository.save(entry);
        }

        return ResponseEntity.ok("Uživatel byl zablokován.");
    }

    @DeleteMapping("/{id}/block")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @Transactional
    public ResponseEntity<String> unblockUser(@PathVariable Long id, Authentication authentication) {
        User blocker = userRepository.getByUsername(authentication.getName());
        blockedUserRepository.deleteByBlocker_IdAndBlocked_Id(blocker.getId(), id);
        return ResponseEntity.ok("Uživatel byl odblokován.");
    }

    @GetMapping("/blocked")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public List<BlockedUserResponse> getBlockedUsers(Authentication authentication) {
        User blocker = userRepository.getByUsername(authentication.getName());
        return blockedUserRepository.findBlockedUsersByBlocker(blocker.getId());
    }
}
