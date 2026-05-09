package com.finrisk.dao.impl;

import com.finrisk.dao.UserDao;
import com.finrisk.exception.EmailAlreadyExistsException;
import com.finrisk.model.User;
import com.finrisk.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserDaoJdbcIT {

    static {
        IntegrationTestSupport.configureTestDatabase();
    }

    @Autowired UserDao userDao;

    @Test
    void save_find_roundTrip_and_uniqueEmail() {
        String email = "it-" + System.nanoTime() + "@example.com";
        User u = new User(null, "IT User", email, null);

        User saved = userDao.save(u);
        assertThat(saved.id()).isNotNull();

        User loaded = userDao.findById(saved.id());
        assertThat(loaded.email()).isEqualTo(saved.email());

        User dup = new User(null, "Dup", saved.email(), null);

        assertThatThrownBy(() -> userDao.save(dup)).isInstanceOf(EmailAlreadyExistsException.class);
    }
}
