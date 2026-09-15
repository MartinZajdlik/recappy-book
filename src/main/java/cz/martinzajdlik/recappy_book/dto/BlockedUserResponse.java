package cz.martinzajdlik.recappy_book.dto;

public class BlockedUserResponse {

    private Long id;
    private String username;

    public BlockedUserResponse(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
}
