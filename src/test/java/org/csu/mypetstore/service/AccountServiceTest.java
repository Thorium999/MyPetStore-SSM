package org.csu.mypetstore.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录校验测试（打真库，密码是种子数据里的 BCrypt 散列）。
 */
@SpringBootTest
class AccountServiceTest {

    private static final String PASSWORD = "PetStore123!";

    @Autowired
    private AccountService accountService;

    @Test
    void acceptsEnabledCustomer() {
        LoginResult result = accountService.authenticate("alice", PASSWORD);

        assertThat(result.isOk()).isTrue();
        assertThat(result.user().username()).isEqualTo("alice");
        assertThat(result.user().displayName()).isEqualTo("林同学");
        assertThat(result.user().isAdmin()).isFalse();
    }

    @Test
    void acceptsAdminAndMarksRole() {
        LoginResult result = accountService.authenticate("admin", PASSWORD);

        assertThat(result.isOk()).isTrue();
        assertThat(result.user().isAdmin()).isTrue();
    }

    @Test
    void rejectsWrongPassword() {
        LoginResult result = accountService.authenticate("alice", "wrong-password");

        assertThat(result.status()).isEqualTo(LoginResult.Status.BAD_CREDENTIALS);
        assertThat(result.user()).isNull();
    }

    @Test
    void rejectsUnknownUser() {
        assertThat(accountService.authenticate("nobody", PASSWORD).status())
                .isEqualTo(LoginResult.Status.BAD_CREDENTIALS);
    }

    @Test
    void rejectsDisabledAccountWithCorrectPassword() {
        assertThat(accountService.authenticate("disabled", PASSWORD).status())
                .isEqualTo(LoginResult.Status.DISABLED);
    }

    @Test
    void doesNotRevealDisabledStatusWhenPasswordIsWrong() {
        assertThat(accountService.authenticate("disabled", "wrong-password").status())
                .isEqualTo(LoginResult.Status.BAD_CREDENTIALS);
    }

    @Test
    void rejectsBlankInput() {
        assertThat(accountService.authenticate(null, PASSWORD).status())
                .isEqualTo(LoginResult.Status.BAD_CREDENTIALS);
        assertThat(accountService.authenticate("  ", PASSWORD).status())
                .isEqualTo(LoginResult.Status.BAD_CREDENTIALS);
        assertThat(accountService.authenticate("alice", "").status())
                .isEqualTo(LoginResult.Status.BAD_CREDENTIALS);
    }
}
