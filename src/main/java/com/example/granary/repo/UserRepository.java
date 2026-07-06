package com.example.granary.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.example.granary.model.User;
import java.util.Optional;


@RepositoryRestResource(path = "packages", collectionResourceRel = "packages")
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}