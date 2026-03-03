package net.projectsync.springboottransactional.repository;

import net.projectsync.springboottransactional.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
